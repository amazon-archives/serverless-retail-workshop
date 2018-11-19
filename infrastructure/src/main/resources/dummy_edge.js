'use strict';

exports.index_rewrite = (event, context, callback) => {
    var request = event.Records[0].cf.request;
    callback(null, request)
}
