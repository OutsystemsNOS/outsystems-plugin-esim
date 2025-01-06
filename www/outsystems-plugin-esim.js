var exec = require('cordova/exec');

exports.eSimAdd = function (success, error, activationCode) {
    exec(success, error, 'eSIM', 'eSimAdd', [activationCode]);
};

exports.isEnabled = function (success, error) {
    exec(success, error, 'eSIM', 'isEnabled', []);
};
