var dbName = db;
var query = {"name": new RegExp("^"+dbName+".room_")};
var roomCollections = db.system.namespaces.find(query).toArray();
if (roomCollections.length > 0) {
  var roomTypes = ["u", "s", "t", "e"];
  for (var i=0; i<roomTypes.length; i++) {
    print("====== Start migrating rooms which type is "+roomTypes[i]);
    migrateRoom(roomTypes[i]);
  }
  if (isCollectionExist(dbName, "room_rooms")) {
    if (!isCollectionExist(dbName, "rooms")) {
      db.room_rooms.renameCollection("rooms");
    } else {
      db.room_rooms.copyTo("rooms");
      db.room_rooms.drop();
    }
    print("====== Migrated room_rooms collection.");
  }
}

function migrateRoom(roomType) {
  if (!isCollectionExist(dbName, "messages_room_"+roomType)) {
    db.createCollection("messages_room_"+roomType);
  }
  var rooms = db.room_rooms.find({"type":roomType});
  // Add 'roomId' field to all documents of a room which type is given
  // Move all documents of that room to room_{type}
  // Remove migrated room
  rooms.forEach(function(room) {
    var roomId = room._id;
    var roomName = "room_"+roomId;
    if (isCollectionExist(dbName, roomName)) {
      var addRoomIdToMessages = "db."+roomName+".update({}, {$set: {\"roomId\": \""+roomId+"\"}}, false, true)";
      eval(addRoomIdToMessages);

      var insertAllMessages = "db."+roomName+".find().forEach(function(doc){db.messages_room_"+roomType+".insert(doc)})";
      eval(insertAllMessages);
    
      var dropRoom = "db."+roomName+".drop()";
      eval(dropRoom);
      print("====== Migrating collection room_"+roomId+" has finished");
    }
  });

  // Remove field 'time' from every documents
  var removeTimeField = "db.messages_room_"+roomType+".update({}, {$unset: {\"time\": 1}}, false, true)";
  eval(removeTimeField);

  print("====== End migrating rooms which type is "+roomType);
  print("==================================================");
}

function isCollectionExist(nameOfDB, collection) {
  var collections = db.system.namespaces.find({"name": nameOfDB+"."+collection}).toArray();
  return (collections.length > 0);
}
