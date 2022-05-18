var exec = require('cordova/exec');

exports.eSimAdd = function (success, error, smdpServerAddress, esimMatchingID) {
    exec(success, error, 'eSIM', 'eSimAdd', [smdpServerAddress, esimMatchingID]);
};
