import uuid

from django.db import models
from django.contrib.auth import get_user_model

from lee_fishing.fishing_net.managers import BasketManager


class Basket(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    created_by = models.ForeignKey(get_user_model(), on_delete=models.DO_NOTHING)

    deleted_at = models.DateTimeField(null=True, blank=True)
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)
    order_id = models.TextField(null=True, blank=True, max_length=500)

    objects = BasketManager()

    def items_with_quantity(self):
        return self.basketitem_set.filter(quantity__gt=0)

    def add_item(self, item):
        if not self.modified_date:
            self.save()

        basket_item, created = self.basketitem_set.update_or_create(
            item_sku=item,
            defaults={
                'basket': self,
                'item_price': item.price,
            }
        )

        basket_item.quantity = 1 if created else basket_item.quantity+1
        basket_item.save()

    def remove_item(self, item):
        if not self.modified_date:
            return

        self.basketitem_set.filter(item_sku=item).update(quantity=0)


class BasketItem(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    basket = models.ForeignKey('Basket', on_delete=models.DO_NOTHING)
    item_sku = models.ForeignKey('fishing_equipment.Product', on_delete=models.DO_NOTHING)
    item_price = models.IntegerField()
    quantity = models.IntegerField(default=0)

    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ('basket', 'item_sku',)

    @property
    def total_price_formatted(self):
        v = self.item_price * self.quantity
        return '$%.2f' % (v / 100)
