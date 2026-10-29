"""Tests for stalls.py. Run with: python3 -m unittest discover -s tools/agents"""

import io
import unittest
import unittest.mock
import urllib.error
from datetime import datetime, timedelta, timezone

import stalls
from inbox import Message
from stalls import Item, current_hold, debate, find, nudge_body, unanswered

T0 = datetime(2026, 10, 29, 9, tzinfo=timezone.utc)
WINDOW = timedelta(minutes=30)
STEP = timedelta(hours=2)
TIMEBOX = timedelta(hours=24)
_n = [0]


def msg(body, minutes=0):
    _n[0] += 1
    return Message("comment", T0 + timedelta(minutes=minutes), f"https://example/c{_n[0]}", body, "TejuBoricha")


def item(messages, debate_issue=False):
    return Item(9, "Title", "https://example/9", debate_issue, list(messages))


def at(minutes):
    return T0 + timedelta(minutes=minutes)


def stalls_of(messages, minutes):
    return unanswered(item(messages), at(minutes), WINDOW)


ASK = "To: opencode\nPlease review.\nFrom: claude"


class UnansweredTest(unittest.TestCase):
    def test_overdue_thirty_minutes_after_the_message_without_a_reply(self):
        self.assertEqual(stalls_of([msg(ASK)], 29), [])
        [stall] = stalls_of([msg(ASK)], 31)
        self.assertIn("no reply from opencode for 0 h 31 min", stall.text)

    def test_a_reply_to_the_sender_or_to_all_answers_it(self):
        for to in ("claude", "all"):
            reply = msg(f"To: {to}\nApproved.\nNo reply needed.\nFrom: opencode", 10)
            self.assertEqual(stalls_of([msg(ASK), reply], 300), [], to)

    def test_a_message_from_the_addressee_to_someone_else_is_not_an_answer(self):
        # Review finding 10: opencode talking to gemini says nothing about claude's request.
        elsewhere = msg("To: gemini\nUnrelated.\nNo reply needed.\nFrom: opencode", 10)
        self.assertEqual(len(stalls_of([msg(ASK), elsewhere], 300)), 1)

    def test_a_reply_is_itself_a_wait_unless_it_needs_no_reply(self):
        findings = msg("To: claude\nTwo must-fix findings.\nFrom: opencode", 10)
        [stall] = stalls_of([msg(ASK), findings], 300)
        self.assertIn("opencode's message to claude", stall.text)

    def test_no_reply_needed_counts_only_on_the_messages_own_lines(self):
        quoting = msg("To: opencode\n> No reply needed.\nBut this one needs an answer.\nFrom: claude")
        self.assertEqual(len(stalls_of([quoting], 300)), 1)

    def test_pausing_after_a_push_and_before_or_after_the_request_is_overdue_at_once(self):
        # Review findings 1 and "pausing before the request": the documented order is push, then `Pausing`.
        pause = msg("To: all\nPausing: usage limit, back at 18:00.\nFrom: opencode", 5)
        self.assertIn("opencode posted `Pausing`", stalls_of([msg(ASK), pause], 10)[0].text)
        early_pause = msg("To: all\nPausing: usage limit.\nFrom: opencode", -60)
        self.assertEqual(len(stalls_of([early_pause, msg(ASK)], 1)), 1)

    def test_coming_back_after_a_pause_ends_it(self):
        pause = msg("To: all\nPausing: usage limit.\nFrom: opencode", 5)
        back = msg("To: all\nBack, reviewing now.\nNo reply needed.\nFrom: opencode", 60)
        self.assertEqual(stalls_of([msg(ASK), pause, back], 300), [])

    def test_a_working_note_holds_until_its_done_by_time_plus_the_window(self):
        # Round-2 finding N5: the stated time counts. T0 is 09:00 UTC; the note says done by 11:00.
        working = msg("To: claude\nWorking: reviewing, done by 11:00 UTC.\nFrom: opencode", 60)
        self.assertEqual(stalls_of([msg(ASK), working], 149), [])
        [stall] = stalls_of([msg(ASK), working], 151)
        self.assertIn("`Working:` note ran out 0 h 01 min ago", stall.text)

    def test_a_working_note_without_a_time_holds_two_hours_and_needs_no_to_line(self):
        working = msg("Working: on it.\nFrom: opencode", 10)
        self.assertEqual(stalls_of([msg(ASK), working], 129), [])
        self.assertEqual(len(stalls_of([msg(ASK), working], 131)), 1)

    def test_a_restarted_wait_is_announced_again(self):
        # Review nit: the de-duplication key includes the deadline's anchor.
        first = stalls_of([msg(ASK)], 31)[0]
        working = msg("To: claude\nWorking: on it.\nFrom: opencode", 40)
        ask = first.item.messages[0]
        second = stalls_of([ask, working], 200)[0]
        self.assertNotEqual(first.key, second.key)

    def test_names_with_a_model_or_a_tool_suffix_still_match(self):
        # Round-2 finding N1: `From: OpenCode (DeepSeek V4)` and `To: Claude Code` are opencode and claude.
        reply = msg("To: Claude Code\nApproved.\nNo reply needed.\nFrom: OpenCode (DeepSeek V4)", 10)
        self.assertEqual(stalls_of([msg(ASK), reply], 300), [])

    def test_the_owner_is_never_waited_on(self):
        # Round-2 finding N2: the owner answers in plain text, without a From: line, and is not on a clock.
        self.assertEqual(stalls_of([msg("To: owner\nShould we ship?\nFrom: claude")], 900), [])
        both = stalls_of([msg("To: opencode, owner\nReview, and owner please confirm.\nFrom: claude")], 900)
        self.assertEqual([s.text.split(" ")[3] for s in both], ["opencode"])

    def test_a_push_alone_is_not_an_answer(self):
        # Review findings 2 and 3: commit dates are not push times, and Assisted-by matching was unreliable.
        self.assertEqual(len(stalls_of([msg(ASK)], 300)), 1)

    def test_withdrawn_by_the_sender_resolves_it(self):
        # Review finding 4: after a fallback review the original request must be able to clear.
        done = msg("To: all\nUsed a fallback reviewer.\nWithdrawn: opencode\nNo reply needed.\nFrom: claude", 40)
        self.assertEqual(stalls_of([msg(ASK), done], 300), [])
        by_someone_else = msg("To: all\nWithdrawn: opencode\nNo reply needed.\nFrom: gemini", 40)
        self.assertEqual(len(stalls_of([msg(ASK), by_someone_else], 300)), 1)

    def test_only_a_takeover_from_the_addressee_by_another_agent_resolves_it(self):
        # Review finding 5.
        right = msg("To: all\nTaking over #9 from opencode: no reply.\nNo reply needed.\nFrom: claude", 40)
        self.assertEqual(stalls_of([msg(ASK), right], 300), [])
        other = msg("To: all\nTaking over #4 from gemini.\nNo reply needed.\nFrom: claude", 40)
        self.assertEqual(len(stalls_of([msg(ASK), other], 300)), 1)
        itself = msg("To: gemini\nTaking over #9 from opencode.\nNo reply needed.\nFrom: opencode", 40)
        self.assertEqual(len(stalls_of([msg(ASK), itself], 300)), 1)

    def test_names_are_read_without_trailing_punctuation(self):
        self.assertEqual(stalls.taken_over_from("Taking over #9 from opencode."), {"opencode"})
        self.assertEqual(stalls.withdrawn("Withdrawn: opencode, gemini."), {"opencode", "gemini"})

    def test_messages_to_all_unsigned_messages_and_nudges_are_not_waits(self):
        status = msg("To: all\nMerged #9.\nFrom: claude")
        unsigned = msg("To: opencode\nno signature")
        nudge = msg("To: opencode\nOverdue\nFrom: agent-status")
        self.assertEqual(stalls_of([status, unsigned, nudge], 600), [])

    def test_only_the_oldest_waiting_message_per_addressee_is_reported(self):
        self.assertEqual(len(stalls_of([msg(ASK), msg(ASK, 10)], 600)), 1)


class HoldTest(unittest.TestCase):
    def test_a_hold_pauses_the_item_until_it_is_lifted(self):
        hold = msg("To: all\nOwner said: wait.\nHold: owner, until the release question is settled\nFrom: claude", 5)
        self.assertIsNotNone(current_hold([msg(ASK), hold]))
        self.assertEqual(find([item([msg(ASK), hold])], at(600), WINDOW, STEP, TIMEBOX).stalls, [])
        lifted = msg("To: all\nOwner said: go.\nHold lifted: owner\nNo reply needed.\nFrom: claude", 10)
        self.assertIsNone(current_hold([hold, lifted]))
        self.assertEqual(len(find([item([msg(ASK), hold, lifted])], at(600), WINDOW, STEP, TIMEBOX).stalls), 1)

    def test_quoted_holds_and_words_that_start_with_hold_are_not_holds(self):
        self.assertIsNone(current_hold([msg("> Hold: owner\nHolding the lock.\nholds: 3")]))

    def test_a_hold_older_than_a_day_is_listed_for_the_owner(self):
        hold = msg("To: all\nHold: owner\nFrom: claude")
        found = find([item([hold])], at(25 * 60), WINDOW, STEP, TIMEBOX)
        self.assertIn("on hold for 25 h", found.needs_owner[0])


class DebateTest(unittest.TestCase):
    proposal = "To: all\nProposal: store one sync value per video.\nFrom: claude"

    def run_debate(self, messages, minutes):
        return debate(item(messages, debate_issue=True), at(minutes), STEP, TIMEBOX)

    def test_critiques_are_overdue_after_two_hours_with_fewer_than_two_critics(self):
        one = msg("Critique: agree with changes.\nFrom: opencode", 30)
        self.assertEqual(self.run_debate([msg(self.proposal), one], 119).stalls, [])
        [stall] = self.run_debate([msg(self.proposal), one], 121).stalls
        self.assertIn("1 of 2 critiques", stall.text)

    def test_the_proposer_cannot_critique_its_own_proposal(self):
        own = msg("Critique: looks fine.\nFrom: claude", 30)
        other = msg("Critique: object.\nFrom: opencode", 31)
        [stall] = self.run_debate([msg(self.proposal), own, other], 300).stalls
        self.assertIn("1 of 2 critiques", stall.text)

    def test_the_rebuttal_is_overdue_two_hours_after_the_last_critique(self):
        messages = [msg(self.proposal), msg("Critique: agree.\nFrom: opencode", 30),
                    msg("Critique: object.\nFrom: gemini", 40)]
        self.assertEqual(self.run_debate(messages, 159).stalls, [])
        [stall] = self.run_debate(messages, 161).stalls
        self.assertIn("has not posted `Rebuttal:`", stall.text)
        messages.append(msg("Rebuttal: changed the plan.\nFrom: claude", 100))
        self.assertEqual(self.run_debate(messages, 200).stalls, [])

    def critiqued(self):
        return [msg(self.proposal), msg("Critique: agree.\nFrom: opencode", 5), msg("Critique: agree.\nFrom: gemini", 6)]

    def test_only_the_proposers_decision_closes_the_debate_and_quotes_do_not(self):
        stray = msg("Critique: object.\nDecision: should be per video\nFrom: opencode", 10)
        quoted = msg("> Decision: per-video storage.\nFrom: gemini", 11)
        self.assertNotEqual(self.run_debate([msg(self.proposal), stray, quoted], 900).stalls, [])
        decision = msg("To: all\nDecision: per-video storage.\nOwner approval needed: no\nFrom: claude", 20)
        self.assertEqual(self.run_debate([*self.critiqued(), decision], 900).stalls, [])

    def test_a_decision_before_two_critiques_does_not_count_unless_overridden(self):
        # Round-2 finding N3: a proposer could skip the critiques.
        decision = msg("To: all\nDecision: per-video storage.\nOwner approval needed: no\nFrom: claude", 2)
        [stall] = self.run_debate([msg(self.proposal), decision], 60).stalls
        self.assertIn("0 of 2 critiques", stall.text)
        override = msg("To: all\nOwner said: skip the critiques, decide now.\nOverride: debate critiques on #9\n"
                       "Follow-up: critiques after the decision\nNo reply needed.\nFrom: claude", 1)
        self.assertEqual(self.run_debate([msg(self.proposal), override, decision], 60).stalls, [])

    def test_a_decision_that_needs_the_owner_is_listed_until_the_owner_answers(self):
        decision = msg("To: all\nDecision: per-video storage.\nOwner approval needed: yes\nFrom: claude", 20)
        found = self.run_debate([*self.critiqued(), decision], 60)
        self.assertIn("waiting for your approval", found.needs_owner[0])
        approval = msg("To: all\nOwner said (in my session): approve.\nNo reply needed.\nFrom: opencode", 30)
        self.assertEqual(self.run_debate([*self.critiqued(), decision, approval], 60).needs_owner, [])

    def test_owner_approval_is_read_only_from_an_owner_said_line(self):
        # Round-2 finding N4: "Owner said nothing" in passing, or a quote, is not an approval; a rejection is not one.
        decision = msg("To: all\nDecision: per-video storage.\nOwner approval needed: yes\nFrom: claude", 20)
        chatter = msg("To: all\nI think the owner said approve to a different plan.\n> Owner said: approve\n"
                      "No reply needed.\nFrom: gemini", 30)
        self.assertEqual(len(self.run_debate([*self.critiqued(), decision, chatter], 60).needs_owner), 1)
        rejected = msg("To: all\nOwner said (in my session): rejected, keep one global value.\nNo reply needed.\n"
                       "From: opencode", 30)
        found = self.run_debate([*self.critiqued(), decision, rejected], 60)
        self.assertEqual(found.needs_owner, [])
        self.assertIn("rejected the decision", found.stalls[0].text)

    def test_a_debate_past_its_time_box_is_flagged(self):
        critiques = [msg("Critique: agree.\nFrom: opencode", 30), msg("Critique: object.\nFrom: gemini", 40)]
        rebuttal = msg("Rebuttal: revised.\nFrom: claude", 60)
        found = self.run_debate([msg(self.proposal), *critiques, rebuttal], 25 * 60)
        self.assertTrue(any("past its time box" in s.text for s in found.stalls))

    def test_an_agreeing_critique_after_two_rounds_is_not_an_objection(self):
        # Round-2 finding N6.
        messages = [msg(self.proposal), msg("Critique: object.\nFrom: opencode", 10),
                    msg("Critique: object.\nFrom: gemini", 11), msg("Rebuttal: one.\nFrom: claude", 20),
                    msg("Critique: still object.\nFrom: opencode", 30), msg("Rebuttal: two.\nFrom: claude", 40),
                    msg("Critique: now agree.\nVerdict: agree\nFrom: opencode", 50)]
        self.assertFalse(any("two rounds are done" in s.text for s in self.run_debate(messages, 60).stalls))

    def test_objections_after_two_rounds_go_to_the_owner(self):
        messages = [msg(self.proposal), msg("Critique: object.\nFrom: opencode", 10),
                    msg("Critique: object.\nFrom: gemini", 11), msg("Rebuttal: one.\nFrom: claude", 20),
                    msg("Critique: still object.\nFrom: opencode", 30), msg("Rebuttal: two.\nFrom: claude", 40),
                    msg("Critique: still object.\nFrom: opencode", 50)]
        found = self.run_debate(messages, 60)
        self.assertTrue(any("two rounds are done" in s.text for s in found.stalls))

    def test_over_long_proposals_and_critiques_are_flagged(self):
        long_proposal = msg("To: all\nProposal: " + "word " * 600 + "\nFrom: claude")
        long_critique = msg("Critique: " + "word " * 400 + "\nFrom: opencode", 10)
        texts = [s.text for s in self.run_debate([long_proposal, long_critique], 20).stalls]
        self.assertTrue(any("over the 500-word limit" in t for t in texts))
        self.assertTrue(any("over the 350-word limit" in t for t in texts))


class OverrideTest(unittest.TestCase):
    def test_overrides_are_listed_with_their_follow_up_until_it_is_done(self):
        override = msg("To: all\nOwner said (in my session): \"skip the review and merge it now, it's urgent\"\n"
                       "Override: independent review on #9\nReason: urgent fix\nScope: this PR only\n"
                       "Follow-up: post-merge review by opencode\nNo reply needed.\nFrom: claude")
        found = find([item([override])], at(10), WINDOW, STEP, TIMEBOX)
        self.assertIn("skipped independent review on \\#9", found.overrides[0])
        self.assertIn("**pending**", found.overrides[0])
        done = msg("To: all\nFollow-up done: reviewed after merge, no findings.\nNo reply needed.\nFrom: opencode", 60)
        self.assertIn(", done", find([item([override, done])], at(70), WINDOW, STEP, TIMEBOX).overrides[0])
        self.assertIn("Overrides in effect", stalls.report([item([override])], found, at(10)))


class ReportTest(unittest.TestCase):
    def test_titles_cannot_mention_people_or_inject_links(self):
        # Review finding 8: a stranger's PR title is rendered into the public status issue.
        pr = Item(3, "@octocat free [x](http://e.vil) `code`", "https://example/3", False, [])
        text = stalls.report([pr], stalls.Findings(), at(0))
        self.assertNotIn("@octocat", text)
        self.assertNotIn("[x](http", text)
        self.assertNotIn("http://e", stalls.safe("see http://e.vil"))

    def test_needs_you_is_the_first_thing_the_owner_reads(self):
        text = stalls.report([], stalls.Findings(needs_owner=["approve #20"]), at(0))
        self.assertLess(text.index("Needs you"), text.index("approve #20"))
        self.assertIn("**Needs you:** nothing.", stalls.report([], stalls.Findings(), at(0)))


class FetchTest(unittest.TestCase):
    """The GitHub side, with a fake API: strangers can neither create waits nor open debates."""

    def test_only_the_owners_comments_and_debates_count(self):
        def comment(n, author, body):
            return {"created_at": "2026-10-29T09:00:00Z", "html_url": f"https://example/c{n}", "body": body,
                    "user": {"login": author}}
        pages = {
            "/pulls?state=open": [{"number": 9, "title": "PR", "html_url": "https://example/9"}],
            "/issues/9/comments": [comment(1, "TejuBoricha", ASK),
                                   comment(2, "stranger", "To: all\nHold: forever\nFrom: claude"),
                                   comment(3, "github-actions[bot]", "To: all\nHold: echoed\nFrom: claude")],
            "/pulls/9/reviews": [],
            "/pulls/9/comments": [],
            "/issues?state=open&labels=debate": [
                {"number": 20, "title": "Owner's", "html_url": "https://example/20", "user": {"login": "tejuboricha"}},
                {"number": 21, "title": "Stranger's", "html_url": "https://example/21", "user": {"login": "stranger"}}],
            "/issues/20/comments": [],
        }

        def fake_get(url, token):
            path = url.split("/repos/TejuBoricha/Aptura", 1)[1].replace("&per_page=100&page=1", "")
            return pages[path.replace("?per_page=100&page=1", "")]

        with unittest.mock.patch.object(stalls.pr_checks, "_get", fake_get):
            items = stalls.fetch("TejuBoricha/Aptura", None)
        self.assertEqual([i.number for i in items], [9, 20])
        self.assertEqual([m.url for m in items[0].messages], ["https://example/c1"])
        self.assertEqual(len(find(items, at(300), WINDOW, STEP, TIMEBOX).stalls), 1,
                         "neither the stranger's nor another workflow's Hold may stop the check")


class PublishTest(unittest.TestCase):
    def publish(self, items, status_issues):
        sent = []
        with unittest.mock.patch.object(stalls, "_send", lambda method, url, token, payload: sent.append((method, url))), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter(status_issues)):
            posted = stalls.publish("o/r", "token", items, find(items, at(300), WINDOW, STEP, TIMEBOX), "report")
        return posted, sent

    def test_posts_each_stall_once_and_updates_the_status_issue(self):
        pr = item([msg(ASK)])
        posted, sent = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 1)
        self.assertEqual(sent, [("POST", "https://api.github.com/repos/o/r/issues/9/comments"),
                                ("PATCH", "https://api.github.com/repos/o/r/issues/50")])
        [stall] = find([pr], at(300), WINDOW, STEP, TIMEBOX).stalls
        pr.messages.append(msg(nudge_body(stall), 200))
        posted, sent = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 0)
        self.assertEqual(sent, [("PATCH", "https://api.github.com/repos/o/r/issues/50")])

    def test_creates_the_status_issue_when_there_is_none(self):
        posted, sent = self.publish([], [])
        self.assertEqual(sent, [("POST", "https://api.github.com/repos/o/r/issues")])

    def test_a_nudge_wakes_every_agent_and_is_not_itself_a_wait(self):
        [stall] = find([item([msg(ASK)])], at(300), WINDOW, STEP, TIMEBOX).stalls
        body = nudge_body(stall)
        self.assertTrue(body.startswith("To: all\n"))
        self.assertTrue(body.endswith("From: agent-status"))
        self.assertEqual(stalls_of([msg(body)], 900), [])


class MainFailureModeTest(unittest.TestCase):
    def run_main(self, fetch, argv=()):
        out = io.StringIO()
        with unittest.mock.patch.object(stalls, "fetch", fetch), \
                unittest.mock.patch.object(stalls.sys, "argv", ["stalls.py", *argv]), \
                unittest.mock.patch.dict(stalls.os.environ, {}, clear=True), \
                unittest.mock.patch.object(stalls.sys, "stdout", out):
            return stalls.main(), out.getvalue()

    @staticmethod
    def http_error(code):
        def fail(repo, token, extra=None):
            raise urllib.error.HTTPError("https://api.github.com/x", code, "error", {}, None)
        return fail

    def test_a_permission_error_fails_loudly(self):
        code, out = self.run_main(self.http_error(403))
        self.assertEqual(code, 1)
        self.assertIn("::error::", out)

    def test_an_outage_is_skipped_with_a_warning(self):
        code, out = self.run_main(self.http_error(503))
        self.assertEqual(code, 0)
        self.assertIn("::warning::", out)

    def test_nudging_without_a_token_is_refused(self):
        code, _ = self.run_main(lambda repo, token, extra=None: [], argv=["--nudge"])
        self.assertEqual(code, 2)

    def test_a_dry_run_reports_without_posting(self):
        ask = Message("comment", datetime.now(timezone.utc) - timedelta(hours=5), "https://example/old", ASK,
                      "TejuBoricha")
        with unittest.mock.patch.object(stalls, "_send") as send:
            code, out = self.run_main(lambda repo, token, extra=None: [item([ask])])
        self.assertEqual(code, 0)
        self.assertIn("**stalled**", out)
        send.assert_not_called()


if __name__ == "__main__":
    unittest.main()
