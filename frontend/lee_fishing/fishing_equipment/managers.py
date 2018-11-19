from django.db import models


class ProductManager(models.Manager):
    def not_deleted(self):
        return self.filter(deleted_at__isnull=True)
