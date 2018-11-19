import os

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
User = get_user_model()


class Command(BaseCommand):
    def handle(self, *args, **options):
        username = os.environ['DEFAULT_USER']
        email = os.environ['DEFAULT_EMAIL']
        password = os.environ['DEFAULT_PASSWORD']
        if not User.objects.filter(username=username).exists():
            User.objects.create_superuser(
                username,
                email,
                password
            )
