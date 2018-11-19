import uuid

from django.db import models
from imagekit.models import ProcessedImageField
from imagekit.processors import ResizeToFill

from lee_fishing.fishing_equipment.managers import ProductManager


class Category(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    title = models.CharField(max_length=100, null=False, blank=False)
    slug = models.SlugField(null=False, blank=False, unique=True)

    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.title

    class Meta:
        ordering = ['title']
        verbose_name_plural = 'Categories'


class Product(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    title = models.CharField(max_length=100, null=False, blank=False)
    description = models.TextField(blank=True)
    slug = models.SlugField(null=False, blank=False, unique=True)

    primary_category = models.ForeignKey(Category,
                                         on_delete=models.DO_NOTHING, null=False, blank=False,
                                         related_name='primary_products'
                                         )
    categories = models.ManyToManyField(Category, blank=True)

    picture = models.ImageField(upload_to='products', null=True, blank=True)
    smaller_picture = ProcessedImageField(upload_to='product_thumbs',
                                          processors=[ResizeToFill(200, 200)],
                                          format='JPEG',
                                          options={'quality': 70},
                                          null=True, blank=True)

    in_stock = models.BooleanField()
    price = models.IntegerField()

    deleted_at = models.DateTimeField(null=True, blank=True)

    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    objects = ProductManager()

    @property
    def formatted_price(self):
        """
        Formatted price (as a string) for this product. There's so much
        wrong with this, you shouldn't use this for any serious
        ecommerce site!

        :return: formatted price... ish
        :rtype: str
        """

        return '$%.2f' % (self.price / 100)

    def __str__(self):
        return self.title

    class Meta:
        ordering = ['title']
