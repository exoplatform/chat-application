var express    = require('express');
var app        = express();
var port       = 3000;

var router = express.Router();
// mock REST service to fetch rooms
router.get('/whoIsOnline', function(req, res) {
    res.set('content-type','application/json; charset=utf8')
    res.sendFile(__dirname+"/src/main/webapp/js/mock/rooms.json");
});
app.use('/chatServer', router);

// set index-dev.html as the default page
/*app.get('/', function(req, res){
    res.sendFile('index-dev.html', { root: __dirname + "/target/chatApplicationVue" } );
});*/
//app.use(express.static(__dirname+"/target/chatApplicationVue/"));

// start the server
app.listen(port);