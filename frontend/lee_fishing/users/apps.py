from django.apps import AppConfig
# noinspection PyUnresolvedReferences
from aws_xray_sdk.core import patch


class UsersAppConfig(AppConfig):

    name = "lee_fishing.users"
    verbose_name = "Users"

    def ready(self):
        try:
            # noinspection PyPackageRequirements
            import users.signals  # noqa F401
        except ImportError:
            pass

        patch(('requests',))
