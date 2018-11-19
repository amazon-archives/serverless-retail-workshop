from django.urls import path

from lee_fishing.fishing_equipment.views import CategoryDetailView, CategoryListView, ProductDetailView

app_name = 'equipment'
urlpatterns = [
    path("", view=CategoryListView.as_view(), name="list"),
    path("<str:slug>/", view=CategoryDetailView.as_view(), name="detail"),
    path("<str:category>/<str:slug>/", view=ProductDetailView.as_view(), name="product"),
]
