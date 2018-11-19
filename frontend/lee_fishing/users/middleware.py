
class DontCacheAuthenticatedMiddleware(object):
    """
    This is a very crude middleware - but we just assume
    that if you're logged in that things are not cached.
    """
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)
        response['Cache-Control'] = 'public, max-age=60'

        try:
            if request.user.is_authenticated:
                response['Cache-Control'] = 'private'

        except AttributeError:
            response['Cache-Control'] = 'private'

        return response
