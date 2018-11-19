from lee_fishing.fishing_net.models import Basket


def current_basket(request):
    basket = None
    if request.user.is_authenticated:
        basket = Basket.objects.current_for_user(request.user)

    return {'current_basket': basket}
