from django.db.models import Count
from django.http import HttpResponse
from django.urls import reverse
from django.views.generic import DetailView, ListView, RedirectView

from lee_fishing.fishing_equipment.models import Product, Category


class RootRedirect(RedirectView):
    permanent = False

    def get(self, request, *args, **kwargs):
        if self.request.META['HTTP_USER_AGENT'] == 'ELB-HealthChecker/2.0':
            return HttpResponse("Move along... nothing to see here.".encode('utf-8'), content_type="text/plain")

        return super(RootRedirect, self).get(request, *args, **kwargs)

    def get_redirect_url(self, *args, **kwargs):
        return reverse('equipment:list')


class ProductDetailView(DetailView):
    queryset = Product.objects.not_deleted()

    def get_queryset(self):
        queryset = super(ProductDetailView, self).get_queryset()
        return queryset.filter(primary_category__slug=self.kwargs['category'])


class CategoryListView(ListView):
    model = Category
    paginate_by = 10

    def get_queryset(self):
        return self.model.objects.annotate(
            products=Count('primary_products')
        ).order_by('-products', 'title')


class CategoryDetailView(ListView):
    model = Category
    paginate_by = 10

    def get_queryset(self):
        return self.model.objects.get(slug=self.kwargs['slug']).primary_products.all()
