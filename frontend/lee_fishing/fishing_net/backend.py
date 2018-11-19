from django.conf import settings
import requests


class BackendClient(object):
    def __init__(self):
        if settings.BACKEND_DOMAIN.startswith("localhost"):
            self._url = 'http://localhost:8080'
        else:
            self._url = ''.join([settings.BACKEND_PROTOCOL, '://', settings.BACKEND_DOMAIN, settings.BACKEND_URI_PREFIX])

    def place_order(self):
        r = requests.post(self._url + '/order', json={})
        return r.text

    def ping(self):
        r = requests.get(self._url + '/ping')
        return r.text
