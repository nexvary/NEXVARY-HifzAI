import tempfile
import unittest
from pathlib import Path
from tools.summarize_test_results import junit_result, instrumentation_result


class ResultSummaryTests(unittest.TestCase):
    def test_actual_junit_cases_must_match_counters(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'TEST-fixture.xml'
            path.write_text('<testsuite tests="2" failures="1" skipped="0" errors="0">'
                            '<testcase name="a"/><testcase name="b"><failure>synthetic</failure></testcase></testsuite>')
            report = junit_result(path)
            self.assertEqual(2, report['tests'])
            self.assertEqual(1, report['failures'])
            path.write_text('<testsuite tests="2" failures="0"><testcase/></testsuite>')
            with self.assertRaises(ValueError):
                junit_result(path)

    def test_undeclared_failures_cannot_look_green(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'TEST-fixture.xml'
            path.write_text('<testsuite tests="1" failures="0"><testcase><failure/></testcase></testsuite>')
            with self.assertRaises(ValueError):
                junit_result(path)

    def test_instrumentation_requires_one_success_and_no_failure_markers(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'instrumentation.txt'
            path.write_text('Time: 1\nOK (17 tests)\nINSTRUMENTATION_CODE: -1\n')
            self.assertEqual(17, instrumentation_result(path)['tests'])
            for invalid in ('shortMsg=Process crashed\n', 'OK (0 tests)\n',
                            'OK (1 test)\nFAILURES!!!\n', 'OK (1 test)\nOK (2 tests)\n'):
                path.write_text(invalid)
                with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                    instrumentation_result(path)
