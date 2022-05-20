var exec = require('cordova/exec');

exports.eSimAdd = function (success, error, activationCode) {
    exec(success, error, 'eSIM', 'eSimAdd', [activationCode]);
};
