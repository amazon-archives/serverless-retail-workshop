from django.urls import path

from lee_fishing.fishing_net.views import (
    BasketDetailView,
    BasketDeleteItemView,
    BasketAddItemView,
    CheckoutView,
    DummyView,
    CongratulationsView)

app_name = "basket"
urlpatterns = [
    path("", view=BasketDetailView.as_view(), name="basket"),
    path("add/<str:item>", view=BasketAddItemView.as_view(), name="add_item"),
    path("delete/<str:item>", view=BasketDeleteItemView.as_view(), name="delete_item"),
    path("checkout", view=CheckoutView.as_view(), name="checkout"),
    path("bye", view=CongratulationsView.as_view(), name="congrats"),
    path("ping", view=DummyView.as_view(), name="ping")
]
