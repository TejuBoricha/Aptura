"""Tests for stalls.py. Run with: python3 -m unittest discover -s tools/agents"""

import io
import unittest
import unittest.mock
import urllib.error
from datetime import datetime, timedelta, timezone

import stalls
from inbox import Message
from stalls import Commit, Item, debate_stalls, find_stalls, nudge_body, on_hold, unanswered

T0 = datetime(2026, 10, 29, 9, tzinfo=timezone.utc)
WINDOW = timedelta(hours=2)


def msg(body, minutes=0, n=[0]):
    n[0] += 1
    return Message("comment", T0 + timedelta(minutes=minutes), f"https://example/c{n[0]}", body)


def item(messages, commits=(), debate=False):
    return Item(9, "Title", "https://example/9", debate, list(messages), list(commits))


def at(minutes):
    return T0 + timedelta(minutes=minutes)


class UnansweredTest(unittest.TestCase):
    ask = "To: opencode\nPlease review.\nFrom: claude"

    def test_a_message_is_not_overdue_before_its_deadline(self):
        self.assertEqual(unanswered(item([msg(self.ask)]), at(119), WINDOW), [])

    def test_a_message_without_reply_or_push_is_overdue_after_two_hours(self):
        [stall] = unanswered(item([msg(self.ask)]), at(121), WINDOW)
        self.assertIn("no reply or push from opencode for 2 h 01 min", stall.text)

    def test_a_reply_answers_it(self):
        reply = msg("To: claude\nApproved. No reply needed.\nFrom: opencode", minutes=30)
        self.assertEqual(unanswered(item([msg(self.ask), reply]), at(300), WINDOW), [])

    def test_a_reply_is_itself_a_wait_for_its_addressee_unless_it_needs_no_reply(self):
        reply = msg("To: claude\nTwo must-fix findings, see below.\nFrom: opencode", minutes=30)
        [stall] = unanswered(item([msg(self.ask), reply]), at(300), WINDOW)
        self.assertIn("opencode's message to claude", stall.text)

    def test_a_push_by_the_addressee_answers_it_but_the_senders_own_push_does_not(self):
        theirs = Commit(at(30), "Fix\n\nAssisted-by: OpenCode (DeepSeek V4)")
        mine = Commit(at(30), "Fix\n\nAssisted-by: Claude Code")
        self.assertEqual(unanswered(item([msg(self.ask)], [theirs]), at(300), WINDOW), [])
        self.assertEqual(len(unanswered(item([msg(self.ask)], [mine]), at(300), WINDOW)), 1)

    def test_a_pausing_note_makes_it_overdue_at_once_until_the_agent_is_back(self):
        pause = msg("To: all\nPausing: usage limit, back at 18:00.\nFrom: opencode", minutes=5)
        [stall] = unanswered(item([msg(self.ask), pause]), at(10), WINDOW)
        self.assertIn("opencode posted `Pausing`", stall.text)
        back = msg("To: all\nBack, reviewing now.\nFrom: opencode", minutes=60)
        self.assertEqual(unanswered(item([msg(self.ask), pause, back]), at(300), WINDOW), [])

    def test_a_working_note_restarts_the_deadline_without_ending_the_wait(self):
        working = msg("To: all\nWorking: reviewing now, done by 11:00 UTC.\nFrom: opencode", minutes=60)
        self.assertEqual(unanswered(item([msg(self.ask), working]), at(179), WINDOW), [])
        [stall] = unanswered(item([msg(self.ask), working]), at(181), WINDOW)
        self.assertIn("`Working:` note is 2 h 01 min old", stall.text)

    def test_a_takeover_resolves_it(self):
        takeover = msg("To: all\nTaking over #9 from opencode.\nFrom: claude", minutes=130)
        self.assertEqual(unanswered(item([msg(self.ask), takeover]), at(300), WINDOW), [])

    def test_messages_to_all_and_the_bots_own_nudges_are_not_waits(self):
        status = msg("To: all\nMerged #9.\nFrom: claude")
        nudge = msg("To: opencode\nOverdue\nFrom: agent-status")
        self.assertEqual(unanswered(item([status, nudge]), at(600), WINDOW), [])

    def test_only_the_oldest_waiting_message_per_addressee_is_reported(self):
        first, second = msg(self.ask), msg(self.ask, minutes=10)
        self.assertEqual(len(unanswered(item([first, second]), at(600), WINDOW)), 1)


class HoldTest(unittest.TestCase):
    def test_a_hold_pauses_the_item_until_it_is_lifted(self):
        ask = msg("To: opencode\nReview?\nFrom: claude")
        hold = msg("To: all\nOwner said: wait.\nHold: owner, until the release question is settled\nFrom: claude", 5)
        self.assertTrue(on_hold([ask, hold]))
        self.assertEqual(find_stalls([item([ask, hold])], at(600), WINDOW), [])
        lifted = msg("To: all\nOwner said: go.\nHold lifted: owner\nFrom: claude", 10)
        self.assertFalse(on_hold([ask, hold, lifted]))
        self.assertEqual(len(find_stalls([item([ask, hold, lifted])], at(600), WINDOW)), 1)

    def test_words_that_merely_start_with_hold_are_not_a_hold(self):
        self.assertFalse(on_hold([msg("Holding the lock until CI is green.\nholds: 3")]))


class DebateTest(unittest.TestCase):
    proposal = "To: all\nProposal: store one sync value per video.\nFrom: claude"

    def test_critiques_are_overdue_after_two_hours_with_fewer_than_two_critics(self):
        one = msg("To: claude\nCritique: agree with changes.\nFrom: opencode", 30)
        self.assertEqual(debate_stalls(item([msg(self.proposal), one], debate=True), at(119), WINDOW), [])
        [stall] = debate_stalls(item([msg(self.proposal), one], debate=True), at(121), WINDOW)
        self.assertIn("1 of 2 critiques", stall.text)

    def test_the_proposer_cannot_critique_its_own_proposal(self):
        self_critique = msg("To: all\nCritique: looks fine.\nFrom: claude", 30)
        other = msg("To: claude\nCritique: object.\nFrom: opencode", 31)
        [stall] = debate_stalls(item([msg(self.proposal), self_critique, other], debate=True), at(300), WINDOW)
        self.assertIn("1 of 2 critiques", stall.text)

    def test_the_rebuttal_is_overdue_two_hours_after_the_last_critique(self):
        critiques = [msg("Critique: agree.\nFrom: opencode", 30), msg("Critique: object.\nFrom: gemini", 40)]
        debate = item([msg(self.proposal), *critiques], debate=True)
        self.assertEqual(debate_stalls(debate, at(159), WINDOW), [])
        [stall] = debate_stalls(debate, at(161), WINDOW)
        self.assertIn("has not posted `Rebuttal:`", stall.text)
        debate.messages.append(msg("Rebuttal: changed the plan.\nFrom: claude", 100))
        self.assertEqual(debate_stalls(debate, at(600), WINDOW), [])

    def test_a_decision_closes_the_debate(self):
        decision = msg("To: all\nDecision: per-video storage.\nFrom: claude", 10)
        self.assertEqual(debate_stalls(item([msg(self.proposal), decision], debate=True), at(900), WINDOW), [])


class NudgeTest(unittest.TestCase):
    def test_a_stall_is_announced_once(self):
        ask = msg("To: opencode\nReview?\nFrom: claude")
        pr = item([ask])
        [stall] = find_stalls([pr], at(300), WINDOW)
        self.assertFalse(stalls.already_announced(pr, stall))
        pr.messages.append(msg(nudge_body(stall), 200))
        self.assertTrue(stalls.already_announced(pr, stall))

    def test_a_nudge_wakes_every_agent_and_is_not_itself_a_wait(self):
        ask = msg("To: opencode\nReview?\nFrom: claude")
        [stall] = find_stalls([item([ask])], at(300), WINDOW)
        body = nudge_body(stall)
        self.assertTrue(body.startswith("To: all\n"))
        self.assertTrue(body.endswith("From: agent-status"))


class PublishTest(unittest.TestCase):
    def publish(self, items, status_issues):
        sent = []
        with unittest.mock.patch.object(stalls, "_send", lambda method, url, token, payload: sent.append((method, url))), \
                unittest.mock.patch.object(stalls.pr_checks, "_pages", lambda url, token, limit=10: iter(status_issues)):
            posted = stalls.publish("o/r", "token", items, find_stalls(items, at(300), WINDOW), "report")
        return posted, sent

    def test_posts_each_stall_once_and_updates_the_status_issue(self):
        pr = item([msg("To: opencode\nReview?\nFrom: claude")])
        posted, sent = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 1)
        self.assertEqual(sent, [("POST", "https://api.github.com/repos/o/r/issues/9/comments"),
                                ("PATCH", "https://api.github.com/repos/o/r/issues/50")])
        [stall] = find_stalls([pr], at(300), WINDOW)
        pr.messages.append(msg(nudge_body(stall), 200))
        posted, sent = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 0)
        self.assertEqual(sent, [("PATCH", "https://api.github.com/repos/o/r/issues/50")])

    def test_creates_the_status_issue_when_there_is_none(self):
        posted, sent = self.publish([], [])
        self.assertEqual(sent, [("POST", "https://api.github.com/repos/o/r/issues")])


class MainFailureModeTest(unittest.TestCase):
    def run_main(self, fetch, argv=(), env=None):
        out = io.StringIO()
        with unittest.mock.patch.object(stalls, "fetch", fetch), \
                unittest.mock.patch.object(stalls.sys, "argv", ["stalls.py", *argv]), \
                unittest.mock.patch.dict(stalls.os.environ, env or {}, clear=True), \
                unittest.mock.patch.object(stalls.sys, "stdout", out):
            return stalls.main(), out.getvalue()

    @staticmethod
    def http_error(code, headers=None):
        def fail(repo, token):
            raise urllib.error.HTTPError("https://api.github.com/x", code, "error", headers or {}, None)
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
        code, out = self.run_main(lambda repo, token: [], argv=["--nudge"])
        self.assertEqual(code, 2)

    def test_a_dry_run_reports_without_posting(self):
        ask = Message("comment", datetime.now(timezone.utc) - timedelta(hours=5), "https://example/old",
                      "To: opencode\nReview?\nFrom: claude")
        with unittest.mock.patch.object(stalls, "_send") as send:
            code, out = self.run_main(lambda repo, token: [item([ask])])
        self.assertEqual(code, 0)
        self.assertIn("**stalled**", out)
        send.assert_not_called()


if __name__ == "__main__":
    unittest.main()
