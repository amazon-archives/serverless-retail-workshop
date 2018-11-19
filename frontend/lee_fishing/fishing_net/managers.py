from django.db import models


class BasketManager(models.Manager):
    def current_for_user(self, user):
        try:
            return self.filter(
                created_by=user, deleted_at__isnull=True, order_id__isnull=True,
            ).latest('created_date')
        except self.model.DoesNotExist:
            return self.model(created_by=user)
