import unittest
from tools.verify_local_only import source_permissions, check_permissions, apk_permissions


class LocalOnlyTests(unittest.TestCase):
    def test_all_current_app_manifests_preserve_offline_scope(self):
        self.assertIn('android.permission.RECORD_AUDIO', source_permissions())

    def test_dependency_merged_network_permissions_cannot_pass(self):
        safe = "package: com.nexvary.hifzai\nuses-permission: name='android.permission.RECORD_AUDIO'\n"
        self.assertEqual({'android.permission.RECORD_AUDIO'}, apk_permissions(safe))
        for permission in ('INTERNET', 'ACCESS_NETWORK_STATE', 'BLUETOOTH_CONNECT'):
            with self.assertRaises(ValueError):
                apk_permissions(safe+"uses-permission: name='android.permission."+permission+"'\n")
        with self.assertRaises(ValueError):
            apk_permissions('unknown aapt output')
        with self.assertRaises(ValueError):
            check_permissions({'android.permission.INTERNET'})
