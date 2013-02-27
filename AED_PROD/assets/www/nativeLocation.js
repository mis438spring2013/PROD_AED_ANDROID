var NativeGeolocation = {
    callNativeFunction: function (success, fail, resultType) {
    	return cordova.exec(success, fail, "edu.iastate.mis.spring.NativeGeo1", "getCurrentPosition", [resultType]);
    }
};