class SimpleUdpPlugin{
	constructor(){
	}

	send(message, host, port){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result);
				},
				function(err){
					reject(err);
				},
				"SimpleUdp", "send",
				[message, host, port]);
		});
	}
	
	receiving(port, callback){
        return new Promise((resolve, reject) =>{
            cordova.exec(
                 function(result){
                     callback(result);
                 },
                 function(err){
                     reject(err);
                 },
                 "SimpleUdp", "receiving",
                 [port]);
        });
	}
}

module.exports = new SimpleUdpPlugin();