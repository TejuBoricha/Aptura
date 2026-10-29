"""Tests for stalls.py. Run with: python3 -m unittest discover -s tools/agents"""

import io
import re
import unittest
from pathlib import Path
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

    def test_only_a_utc_time_counts_in_a_working_note(self):
        # Round-3 nit: "by 2:30pm" or a local time is not read as UTC.
        local = msg("To: claude\nWorking: review, done by 2:30pm CET.\nFrom: opencode", 60)
        self.assertEqual(len(stalls_of([msg(ASK), local], 181)), 1)

    def test_a_working_note_on_another_question_does_not_reopen_an_answered_one(self):
        # Round-3 finding S6.
        answer = msg("To: claude\nDone, approved.\nNo reply needed.\nFrom: opencode", 5)
        working = msg("To: all\nWorking: on the next task, done by 18:00 UTC.\nFrom: opencode", 20)
        self.assertEqual(stalls_of([msg(ASK), answer, working], 24 * 60), [])

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


class AuditParsingTest(unittest.TestCase):
    """Audit T3, T4 and T6: what the checks read, and edge cases in the wait rules."""

    def test_markers_in_code_blocks_never_count(self):
        example = "To: all\nThe format is:\n```\nHold: example\nOverride: review on #9\nFrom: opencode\n```\nFrom: claude"
        self.assertIsNone(current_hold([msg(example)]))
        self.assertIsNone(stalls.override_step(msg(example)))
        self.assertEqual(stalls.sender(example), "claude")

    def test_common_formatting_is_understood(self):
        self.assertEqual(stalls.recipients("**To:** opencode\nhi\n**From:** claude"), {"opencode"})
        self.assertEqual(stalls.sender("**To:** opencode\nhi\n**From:** claude"), "claude")
        self.assertTrue(stalls.has_line("- Working: on it, done by 11:00 UTC", "working"))
        self.assertEqual(stalls.recipients("To: claude and opencode\nhi\nFrom: gemini"), {"claude", "opencode"})

    def test_an_unreadable_message_is_reported_once(self):
        greeting = msg("Hi opencode!\nTo: opencode\nPlease review.\nFrom: claude")
        unsigned = msg("To: opencode\nPlease review.", 1)
        found = find([item([greeting, unsigned])], at(5), WINDOW, STEP, TIMEBOX)
        texts = " ".join(s.text for s in found.stalls)
        self.assertIn("`To:` line is not the first line", texts)
        self.assertIn("no `From:` line", texts)
        self.assertNotIn("cannot be read", " ".join(
            s.text for s in find([item([msg(ASK)])], at(5), WINDOW, STEP, TIMEBOX).stalls))

    def test_a_pause_after_an_answer_does_not_reopen_it(self):
        answer = msg("To: claude\nDone.\nNo reply needed.\nFrom: opencode", 5)
        pause = msg("To: all\nPausing: usage limit.\nFrom: opencode", 20)
        self.assertEqual(stalls_of([msg(ASK), answer, pause], 25), [])

    def test_a_working_time_already_past_or_too_far_falls_back_to_two_hours(self):
        # 10:01 "by 09:00 UTC" would otherwise hold until 09:30 tomorrow.
        posted = T0.replace(hour=10, minute=1)
        self.assertEqual(stalls.working_until("Working: x, by 09:00 UTC", posted, WINDOW), posted + timedelta(hours=2))
        self.assertEqual(stalls.working_until("Working: x, by 11:00 UTC", posted, WINDOW),
                         T0.replace(hour=11, minute=30))

    def test_agreed_is_agreement(self):
        self.assertEqual(stalls.verdict("Verdict: agreed"), "agree")
        self.assertEqual(stalls.verdict("Critique: agreed, nothing to add."), "agree")

    def test_a_stall_nobody_resolves_for_a_day_goes_to_the_owner(self):
        pr = item([msg(ASK)])
        [stall] = find([pr], at(60), WINDOW, STEP, TIMEBOX).stalls
        pr.messages.append(msg(nudge_body(stall), 60))
        self.assertEqual(find([pr], at(60 + 23 * 60), WINDOW, STEP, TIMEBOX).needs_owner, [])
        self.assertIn("stalled for 24 h", find([pr], at(60 + 24 * 60), WINDOW, STEP, TIMEBOX).needs_owner[0])

    def test_words_recorded_in_the_owners_name_are_listed_for_the_owner(self):
        # Audit A1: every agent posts as the owner, so the owner sees what was recorded in their name.
        said = msg('To: all\nOwner said (in my session): "approve"\nNo reply needed.\nFrom: claude')
        found = find([item([said])], at(10), WINDOW, STEP, TIMEBOX)
        self.assertIn("by claude", found.owner_words[0])
        self.assertIn("Recorded as your words", stalls.report([item([said])], found, at(10)))

    def test_explain_shows_how_a_message_is_read(self):
        out = io.StringIO()
        with unittest.mock.patch.object(stalls.sys, "stdout", out):
            code = stalls.explain("To: opencode\nWorking: reviewing, done by 11:00 UTC\nFrom: claude")
        self.assertEqual(code, 0)
        self.assertIn("to:       opencode", out.getvalue())
        self.assertIn("markers:  working", out.getvalue())
        self.assertIn("is a wait: False", out.getvalue())
        with unittest.mock.patch.object(stalls.sys, "stdout", io.StringIO()):
            self.assertEqual(stalls.explain("To: opencode\nno signature"), 1)


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
        override = msg("To: all\nOwner said: \"skip the critiques, decide now\"\nOverride: debate critiques on #9\n"
                       "Follow-up: critiques after the decision\nNo reply needed.\nFrom: claude", 1)
        self.assertEqual(self.run_debate([msg(self.proposal), override, decision], 60).stalls, [])

    def test_only_a_verified_override_of_the_critiques_unlocks_an_early_decision(self):
        # Round-3 findings S1 and S2.
        decision = msg("To: all\nDecision: per-video storage.\nOwner approval needed: no\nFrom: claude", 2)
        unquoted = msg("To: all\nOwner said: skip the critiques.\nOverride: debate critiques on #9\n"
                       "Follow-up: none\nNo reply needed.\nFrom: claude", 1)
        other_step = msg("To: all\nOwner said: \"don't wait\"\nOverride: the 30-minute deadline on #9\n"
                         "Follow-up: none\nNo reply needed.\nFrom: claude", 1)
        for override in (unquoted, other_step):
            self.assertEqual(len(self.run_debate([msg(self.proposal), override, decision], 60).stalls), 1)

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
        rejected = msg("To: all\nOwner said (in my session): \"reject, keep one global value\"\nNo reply needed.\n"
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


class OwnerAnswerTest(unittest.TestCase):
    def test_the_answer_is_the_first_word_of_the_quoted_owner_words(self):
        # Round-3 finding S5: contractions and "no" in the middle no longer flip the answer.
        cases = {
            'Owner said: "approve, don\'t change anything"': "approve",
            'Owner said: "yes, but reject option B"': "approve",
            'Owner said: "no"': "reject",
            'Owner said: "reject"': "reject",
            'Owner said: "I do not object"': "unclear",
            'Owner said: "not yet"': "unclear",
        }
        for line, expected in cases.items():
            self.assertEqual(stalls.owner_answer(msg(f"To: all\n{line}\nFrom: claude")), expected, line)

    def test_an_override_quote_is_not_an_answer_to_a_decision(self):
        override = msg('To: all\nOwner said: "go ahead and merge"\nOverride: review on #9\nFrom: claude')
        self.assertIsNone(stalls.owner_answer(override))

    def test_an_unclear_answer_is_shown_to_the_owner(self):
        proposal = msg("To: all\nProposal: x.\nFrom: claude")
        crits = [msg("Critique: agree.\nFrom: opencode", 5), msg("Critique: agree.\nFrom: gemini", 6)]
        decision = msg("To: all\nDecision: x.\nOwner approval needed: yes\nFrom: claude", 20)
        unclear = msg('To: all\nOwner said: "not yet"\nNo reply needed.\nFrom: claude', 30)
        found = debate(item([proposal, *crits, decision, unclear], True), at(60), STEP, TIMEBOX)
        self.assertIn("unclear", found.needs_owner[0])


class VerdictTest(unittest.TestCase):
    def test_verdicts_are_read_as_whole_words(self):
        # Round-3 finding S4: "disagree" contains "agree".
        for text, expected in {"Verdict: disagree": "object", "Verdict: I do not agree": "object",
                               "Verdict: I don't agree": "object", "Critique: I can't agree with this.": "object",
                               "Verdict: agree with changes": "changes", "Verdict: agree": "agree",
                               "Critique: unclear": "object"}.items():
            self.assertEqual(stalls.verdict(text), expected, text)


class OverrideTest(unittest.TestCase):
    def test_overrides_are_listed_with_their_follow_up_until_it_is_done(self):
        override = msg("To: all\nOwner said (in my session): \"skip the review and merge it now, it's urgent\"\n"
                       "Override: independent review on #9\nReason: urgent fix\nScope: this PR only\n"
                       "Follow-up: post-merge review by opencode\nNo reply needed.\nFrom: claude")
        found = find([item([override])], at(10), WINDOW, STEP, TIMEBOX)
        self.assertIn("skipped independent review on \\#9", found.overrides[0])
        self.assertIn("**pending**", found.overrides[0])
        self.assertIn("Overrides in effect", stalls.report([item([override])], found, at(10)))
        done = msg("To: all\nFollow-up done: reviewed after merge, no findings.\nNo reply needed.\nFrom: opencode", 60)
        self.assertEqual(find([item([override, done])], at(70), WINDOW, STEP, TIMEBOX).overrides, [])

    def test_a_pending_follow_up_is_labelled_and_the_label_goes_when_it_is_done(self):
        # Round-4 finding N1: the label keeps a closed item in view at any age.
        override = msg('To: all\nOwner said: "merge now"\nOverride: review on #9\nFollow-up: post-merge review\n'
                       'From: claude')
        pr = item([override])
        self.assertEqual(find([pr], at(10), WINDOW, STEP, TIMEBOX).label, {9})
        pr.labels = {"follow-up"}
        self.assertEqual(find([pr], at(10), WINDOW, STEP, TIMEBOX).label, set())
        pr.messages.append(msg("To: all\nFollow-up done: fine.\nNo reply needed.\nFrom: opencode", 60))
        self.assertEqual(find([pr], at(70), WINDOW, STEP, TIMEBOX).unlabel, {9})

    def test_an_override_without_the_owners_quoted_words_is_marked_unverified(self):
        unquoted = msg("To: all\nOwner said: skip the review.\nOverride: review on #9\nFollow-up: none\nFrom: claude")
        self.assertIn("unverified", find([item([unquoted])], at(10), WINDOW, STEP, TIMEBOX).overrides[0])

    def test_a_pending_follow_up_stays_listed_after_the_pull_request_closes(self):
        # Round-3 finding M1: "skip the review, review after the merge" is the normal case.
        override = msg('To: all\nOwner said: "merge now"\nOverride: review on #9\nFollow-up: post-merge review\n'
                       'From: claude')
        closed = Item(9, "Merged", "https://example/9", False, [override], is_open=False)
        found = find([closed], at(600), WINDOW, STEP, TIMEBOX)
        self.assertIn("**pending**", found.overrides[0])
        self.assertEqual(found.stalls, [])
        self.assertNotIn("Merged", stalls.report([closed], found, at(600)).split("Overrides in effect")[0])
        done = msg("To: all\nFollow-up done: no findings.\nNo reply needed.\nFrom: opencode", 60)
        closed.messages.append(done)
        self.assertEqual(find([closed], at(600), WINDOW, STEP, TIMEBOX).overrides, [])


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


class FakeGitHub:
    """A fake of the GitHub REST API the scripts use: answers by path, counts calls."""

    def __init__(self, routes):
        self.routes, self.calls = routes, []

    def get(self, url, token):
        path = url.split("/repos/TejuBoricha/Aptura", 1)[1]
        self.calls.append(path)
        path = re.sub(r"[?&]per_page=100&page=\d+", "", path)
        path = re.sub(r"[?&]since=[^&]+", "", path).replace("&sort=created&direction=asc", "").replace(
            "?sort=created&direction=asc", "")
        if path not in self.routes:
            raise AssertionError(f"unexpected request {path}")
        return self.routes[path]


def gh_comment(n, author, body, number, kind="issue", created="2026-10-29T09:00:00Z"):
    key = "issue_url" if kind == "issue" else "pull_request_url"
    path = "issues" if kind == "issue" else "pulls"
    return {"created_at": created, "html_url": f"https://example/c{n}", "body": body, "user": {"login": author},
            key: f"https://api.github.com/repos/TejuBoricha/Aptura/{path}/{number}"}


def pr_entry(n, title="PR", created="2026-10-29T08:00:00Z", body=""):
    return {"number": n, "title": title, "html_url": f"https://example/{n}", "created_at": created,
            "updated_at": created, "body": body, "head": {"sha": f"sha{n}"}, "labels": [],
            "user": {"login": "TejuBoricha"}}


class FetchTest(unittest.TestCase):
    """The GitHub side, with a fake API."""

    def routes(self, **extra):
        base = {
            "/pulls?state=open": [],
            "/issues?state=open": [],
            "/pulls?state=closed&sort=updated&direction=desc": [],
            "/issues?state=closed&labels=debate&sort=updated&direction=desc": [],
            "/issues?state=closed&labels=follow-up": [],
            "/issues/comments": [],
            "/pulls/comments": [],
        }
        base.update(extra)
        return base

    def fetch(self, routes, now=None):
        fake = FakeGitHub(routes)
        with unittest.mock.patch.object(stalls.pr_checks, "_get", fake.get):
            items = stalls.fetch("TejuBoricha/Aptura", None, now=now or at(0))
        return items, fake

    def test_only_the_owners_comments_and_debates_count(self):
        routes = self.routes(**{
            "/pulls?state=open": [pr_entry(9), {**pr_entry(12, "Stranger's PR"), "user": {"login": "stranger"}},
                                  {**pr_entry(13, "Bump x"), "user": {"login": "dependabot[bot]"}}],
            "/pulls/9/reviews": [],
            "/issues?state=open": [
                {**pr_entry(20, "Owner's"), "user": {"login": "tejuboricha"}, "labels": [{"name": "debate"}]},
                {**pr_entry(21, "Stranger's"), "user": {"login": "stranger"}, "labels": [{"name": "debate"}]}],
            "/issues/comments": [gh_comment(1, "TejuBoricha", ASK, 9),
                                 gh_comment(2, "stranger", "To: all\nHold: forever\nFrom: claude", 9),
                                 gh_comment(3, "github-actions[bot]", "To: all\nHold: echoed\nFrom: claude", 9)],
        })
        items, _ = self.fetch(routes)
        self.assertEqual([(i.number, i.kind) for i in items], [(9, "PR"), (20, "Debate")])
        self.assertEqual([m.url for m in items[0].messages], ["https://example/c1"])
        self.assertEqual(len(find(items, at(300), WINDOW, STEP, TIMEBOX).stalls), 1,
                         "neither the stranger's nor another workflow's Hold may stop the check")

    def test_closed_items_are_read_for_overrides_recent_or_labelled(self):
        override = 'To: all\nOwner said: "merge"\nOverride: review on #{n}\nFollow-up: review later\nFrom: claude'
        routes = self.routes(**{
            "/pulls?state=closed&sort=updated&direction=desc": [
                {**pr_entry(8, "Merged"), "updated_at": "2026-10-29T08:00:00Z"},
                {**pr_entry(7, "Old", created="2026-08-01T00:00:00Z"), "updated_at": "2026-09-01T08:00:00Z"}],
            "/issues?state=closed&labels=debate&sort=updated&direction=desc": [
                {**pr_entry(22, "Decided"), "updated_at": "2026-10-28T08:00:00Z"}],
            "/issues?state=closed&labels=follow-up": [
                {**pr_entry(5, "Ancient", created="2026-01-01T00:00:00Z"), "updated_at": "2026-01-02T00:00:00Z",
                 "pull_request": {}, "labels": [{"name": "follow-up"}]}],
            "/issues/comments": [gh_comment(4, "TejuBoricha", override.format(n=8), 8),
                                 gh_comment(6, "TejuBoricha", override.format(n=22), 22)],
            # #5 is older than the repository-wide read, so it is read on its own
            "/issues/5/comments": [gh_comment(5, "TejuBoricha", override.format(n=5), 5)],
            "/pulls/5/reviews": [],
            "/pulls/5/comments": [],
        })
        items, _ = self.fetch(routes)
        self.assertEqual([(i.number, i.is_open) for i in items], [(5, False), (8, False), (22, False)],
                         "#8 and debate #22 closed recently; #5, however old, carries the follow-up label")

    def test_a_claimed_task_without_a_pull_request_is_a_stall_after_two_hours(self):
        # Audit A3: an agent that claims a task and stops left a permanent claim nobody noticed.
        claimed = {**pr_entry(30, "Sleep timer presets"), "labels": [{"name": "claimed:opencode"}]}
        claim = gh_comment(7, "TejuBoricha", "claim: opencode (deepseek)", 30, created="2026-10-29T09:00:00Z")
        items, _ = self.fetch(self.routes(**{"/issues?state=open": [claimed], "/issues/comments": [claim]}))
        [task] = items
        self.assertEqual((task.kind, task.linked), ("Task", False))
        self.assertEqual(find(items, at(119), WINDOW, STEP, TIMEBOX).stalls, [])
        [stall] = find(items, at(121), WINDOW, STEP, TIMEBOX).stalls
        self.assertIn("no open pull request mentions it", stall.text)
        linked, _ = self.fetch(self.routes(**{"/issues?state=open": [claimed], "/issues/comments": [claim],
                                              "/pulls?state=open": [pr_entry(31, body="Refs: #30")],
                                              "/pulls/31/reviews": []}))
        self.assertTrue(next(i for i in linked if i.number == 30).linked)

    def test_a_quiet_pull_request_is_a_stall_after_four_hours_counting_commits(self):
        # Audit A3: a driver that stops without a message left a PR marked "moving".
        routes = self.routes(**{"/pulls?state=open": [pr_entry(9, created="2026-10-29T03:00:00Z")],
                                "/pulls/9/reviews": [],
                                "/commits/sha9": {"commit": {"committer": {"date": "2026-10-29T06:00:00Z"}}}})
        items, fake = self.fetch(routes, now=at(0))
        self.assertIn("/commits/sha9", fake.calls)
        self.assertEqual(items[0].idle_since, datetime(2026, 10, 29, 6, tzinfo=timezone.utc))
        self.assertEqual(find(items, at(0), WINDOW, STEP, TIMEBOX).stalls, [])  # 3 h quiet
        [stall] = find(items, at(61), WINDOW, STEP, TIMEBOX).stalls
        self.assertIn("no message or commit for 4 h 01 min", stall.text)

    def test_the_api_budget_fits_at_scale(self):
        # Audit T1: 30 open and 40 recently closed pull requests cost 215 requests a run before; the workflow token
        # has 1,000 an hour, shared with CI, and the check runs four times an hour.
        open_prs = [pr_entry(n) for n in range(100, 130)]
        closed = [{**pr_entry(n), "updated_at": "2026-10-28T00:00:00Z"} for n in range(200, 240)]
        routes = self.routes(**{"/pulls?state=open": open_prs,
                                "/pulls?state=closed&sort=updated&direction=desc": closed,
                                **{f"/pulls/{n}/reviews": [] for n in range(100, 130)}})
        _, fake = self.fetch(routes, now=at(0))
        self.assertLessEqual(len(fake.calls), 40, fake.calls)


class PublishTest(unittest.TestCase):
    def publish(self, items, status_issues, send=None):
        sent = []
        def default_send(method, url, token, payload):
            sent.append((method, url))
        with unittest.mock.patch.object(stalls, "_send", send or default_send), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter(status_issues)):
            posted, failures = stalls.publish("o/r", "token", items, find(items, at(300), WINDOW, STEP, TIMEBOX),
                                              "report")
        return posted, sent, failures

    def test_posts_each_stall_once_and_updates_the_status_issue(self):
        pr = item([msg(ASK)])
        posted, sent, _ = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 1)
        self.assertEqual(sent, [("POST", "https://api.github.com/repos/o/r/issues/9/comments"),
                                ("PATCH", "https://api.github.com/repos/o/r/issues/50")])
        stalls_now = find([pr], at(300), WINDOW, STEP, TIMEBOX).stalls
        pr.messages.append(msg(nudge_body(stalls_now), 200))
        posted, sent, _ = self.publish([pr], [{"number": 50}])
        self.assertEqual(posted, 0)
        self.assertEqual(sent, [("PATCH", "https://api.github.com/repos/o/r/issues/50")])

    def test_several_new_stalls_on_one_item_make_one_comment(self):
        # Audit U3: one notification per item, not one per stall.
        two = item([msg(ASK), msg("To: gemini\nAlso review.\nFrom: claude", 1)])
        posted, sent, _ = self.publish([two], [{"number": 50}])
        self.assertEqual(posted, 1)
        self.assertEqual(sum(1 for method, url in sent if url.endswith("/comments")), 1)

    def test_a_failed_write_does_not_stop_the_others(self):
        # Audit T5: a locked conversation used to freeze the whole status page.
        sent = []
        def send(method, url, token, payload):
            if url.endswith("/issues/9/comments"):
                raise urllib.error.HTTPError(url, 403, "locked", {}, None)
            sent.append((method, url))
        a, b = item([msg(ASK)]), Item(10, "Other", "https://example/10", False, [msg(ASK)])
        posted, _, failures = self.publish([a, b], [{"number": 50}], send)
        self.assertEqual(posted, 1)
        self.assertEqual(len(failures), 1)
        self.assertIn(("PATCH", "https://api.github.com/repos/o/r/issues/50"), sent)

    def test_the_status_body_stays_under_githubs_limit(self):
        sent = []
        with unittest.mock.patch.object(stalls, "_send", lambda m, u, t, p: sent.append(p)), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter([{"number": 50}])):
            stalls.publish("o/r", "token", [], stalls.Findings(), "x" * 100_000)
        self.assertLessEqual(len(sent[-1]["body"]), 65_536)

    def test_applies_and_removes_the_follow_up_label(self):
        override = msg('To: all\nOwner said: "merge now"\nOverride: review on #9\nFollow-up: review\nFrom: claude')
        sent = []
        found = find([item([override])], at(300), WINDOW, STEP, TIMEBOX)
        found.unlabel.add(4)
        with unittest.mock.patch.object(stalls, "_send", lambda method, url, token, payload: sent.append((method, url))), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter([{"number": 50}])):
            stalls.publish("o/r", "token", [], found, "report")
        self.assertIn(("POST", "https://api.github.com/repos/o/r/issues/9/labels"), sent)
        self.assertIn(("DELETE", "https://api.github.com/repos/o/r/issues/4/labels/follow-up"), sent)

    def test_a_new_needs_you_entry_notifies_the_owner_once(self):
        # Audit U2: editing the status page sends no notification.
        found = stalls.Findings(needs_owner=["[#20](u) decision waiting for your approval: v"])
        sent = []
        def send(method, url, token, payload):
            sent.append((method, url, payload))
        with unittest.mock.patch.object(stalls, "_send", send), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter([{"number": 50, "body": "old"}])):
            stalls.publish("o/r", "token", [], found, "report")
        comments = [p for m, u, p in sent if u.endswith("/issues/50/comments")]
        self.assertEqual(len(comments), 1)
        self.assertIn("decision waiting for your approval", comments[0]["body"])
        sent.clear()
        already = {"number": 50, "body": "- [#20](u) decision waiting for your approval: v"}
        with unittest.mock.patch.object(stalls, "_send", send), \
                unittest.mock.patch.object(stalls, "every", lambda url, token: iter([already])):
            stalls.publish("o/r", "token", [], found, "report")
        self.assertFalse([u for m, u, p in sent if u.endswith("/comments")])

    def test_creates_the_status_issue_when_there_is_none(self):
        _, sent, _ = self.publish([], [])
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


class DriftTest(unittest.TestCase):
    """The rules exist twice, in the docs agents read and in the code that enforces them. These fail when the two
    disagree (audit A5), so a change to one cannot silently leave the other wrong."""

    root = Path(stalls.__file__).resolve().parents[2]

    def text(self, name):
        return (self.root / name).read_text(encoding="utf-8")

    def test_the_documented_deadlines_and_limits_are_the_codes(self):
        docs = self.text("docs/AGENT_WORKFLOW.md")
        defaults = {a.dest: a.default for a in stalls_parser()._actions if a.dest != "help"}
        self.assertIn(f"{int(defaults['minutes'])} min", docs)
        self.assertIn(f"{int(defaults['debate_hours'])} h after the proposal", docs)
        self.assertIn(f"{int(defaults['timebox_hours'])} h, and two rounds", docs)
        self.assertIn(f"at most {stalls.PROPOSAL_WORDS} words", docs)
        self.assertIn(f"at most {stalls.CRITIQUE_WORDS} words", docs)
        self.assertIn(f"{int(stalls.CLAIM_WITHOUT_PR.total_seconds() // 3600)} h after the `claim:` comment", docs)
        self.assertIn(f"{int(stalls.IDLE.total_seconds() // 3600)} h without a message or commit", docs)

    def test_every_documented_agent_name_has_a_claim_label(self):
        labels = re.search(r"for tool in ([a-z ]+); do", self.text(".github/workflows/labels.yml")).group(1).split()
        names = re.findall(r"`([a-z]+)`", re.search(r"one stable name each: (.+)\.", self.text(
            "docs/AGENT_WORKFLOW.md")).group(1))
        self.assertEqual(sorted(labels), sorted(names))

    def test_the_cheat_sheet_says_what_the_parser_reads(self):
        sheet = re.search(r"### Everyday messages.*?```\n(.+?)```", self.text("AGENTS.md"), re.S).group(1)
        lines = [re.split(r"\s{2,}", line)[0] for line in sheet.splitlines() if line.strip()]
        message = "\n".join(lines[:3])
        self.assertEqual((stalls.recipients(message), stalls.sender(message)), ({"opencode"}, "claude"))
        self.assertTrue(stalls.has_line(lines[3], "working"))
        self.assertEqual(stalls.working_until(lines[3], T0.replace(hour=12), WINDOW), T0.replace(hour=15, minute=30))
        self.assertTrue(stalls.has_line(lines[4], "pausing"))
        self.assertTrue(stalls.no_reply_needed(lines[5]))
        self.assertEqual(stalls.taken_over_from(lines[6]), {"opencode"})


def stalls_parser():
    """The argument parser main() builds, captured without running main()."""
    captured = {}
    real = stalls.argparse.ArgumentParser.parse_args

    def grab(self, *a, **k):
        captured["parser"] = self
        raise SystemExit(0)
    with unittest.mock.patch.object(stalls.argparse.ArgumentParser, "parse_args", grab), \
            unittest.mock.patch.object(stalls, "force_utf8_output", lambda: None):
        try:
            stalls.main()
        except SystemExit:
            pass
    return captured["parser"]


if __name__ == "__main__":
    unittest.main()
