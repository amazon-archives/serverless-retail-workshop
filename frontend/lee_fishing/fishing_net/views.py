from timeit import default_timer

from django.contrib import messages
from django.contrib.auth.mixins import LoginRequiredMixin
from django.http import HttpResponseRedirect
from django.urls import reverse
from django.views.generic import TemplateView, DetailView, RedirectView

from lee_fishing.fishing_equipment.models import Product
from lee_fishing.fishing_net.backend import BackendClient
from lee_fishing.fishing_net.models import Basket


class DummyView(TemplateView):
    template_name = 'fishing_net/ping.html'

    def get(self, request, *args, **kwargs):
        backend_client = BackendClient()
        kwargs['pong'] = backend_client.ping()

        return super().get(request, *args, **kwargs)


class BasketDetailView(LoginRequiredMixin, DetailView):
    model = Basket

    def get_object(self, queryset=None):
        return self.model.objects.current_for_user(self.request.user)


class BasketDeleteItemView(LoginRequiredMixin, RedirectView):
    permanent = False

    def get_redirect_url(self, *args, **kwargs):
        basket = Basket.objects.current_for_user(self.request.user)
        basket.remove_item(Product.objects.get(slug=self.kwargs['item']))
        return reverse('baskets:basket')


class BasketAddItemView(LoginRequiredMixin, RedirectView):
    permanent = False

    def get_redirect_url(self, *args, **kwargs):
        basket = Basket.objects.current_for_user(self.request.user)
        basket.add_item(Product.objects.not_deleted().get(slug=self.kwargs['item']))
        return reverse('baskets:basket')


class CheckoutView(LoginRequiredMixin, TemplateView):
    template_name = 'fishing_net/checkout.html'

    def get(self, request, *args, **kwargs):
        return super().get(request, *args, **kwargs)

    def post(self, request, *args, **kwargs):
        start = default_timer()
        basket = Basket.objects.current_for_user(self.request.user)
        backend_client = BackendClient()

        r = backend_client.place_order()
        basket.order_id = r
        basket.save()

        end = default_timer()

        messages.add_message(request, messages.INFO, "It took " + str(end-start) + " seconds to process the order")

        return HttpResponseRedirect(reverse('basket:congrats'))


class CongratulationsView(LoginRequiredMixin, TemplateView):
    template_name = 'fishing_net/congrats.html'
