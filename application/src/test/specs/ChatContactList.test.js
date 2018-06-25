import { shallow } from 'vue-test-utils';
import ChatContactList from '../../main/webapp/vue-app/components/ChatContactList';
import ChatContact from '../../main/webapp/vue-app/components/ChatContact';
import {chatConstants} from '../../main/webapp/vue-app/chatConstants.js';

describe('ChatContactList.test.js', () => {
  let cmp = shallow(ChatContactList, {
    propsData: {
      isSearchingContact : false,
      selected: {
        "fullName":"Test User",
        "unreadTotal":0,
        "isActive":"true",
        "type":"u",
        "user":"testuser",
        "room":"eb74205830cf97546269bbdc5d439b29ddd1735b",
        "status":"invisible",
        "timestamp":1528455913624,
        "isFavorite":false
      },
      contacts: [
          {
            "fullName":"Test User",
            "unreadTotal":0,
            "isActive":"true",
            "type":"u",
            "user":"testuser",
            "room":"eb74205830cf97546269bbdc5d439b29ddd1735b",
            "status":"invisible",
            "timestamp":1528455913624,
            "isFavorite":false
         },
         {
            "lastMessage":{  
               "msg":"Test Message",
               "isSystem":false,
               "options":{},
               "msgId":"5b1a7a4067c9a30c23b5223d",
               "fullname":"Test User 2",
               "type":null,
               "user":"testuser2",
               "timestamp":1528461888213
            },
            "fullName":"Test User 2",
            "unreadTotal":0,
            "isActive":"true",
            "type":"u",
            "user":"testuser2",
            "room":"ca5dc389cc131008d504a44e64dc3d06279a93ae",
            "status":"invisible",
            "timestamp":1528461888215,
            "isFavorite":true
         },
         {
            "lastMessage":{
               "msg":"",
               "isSystem":true,
               "options":{
                  "fullname":"Root Root",
                  "type":"type-add-team-user"
               },
               "msgId":"5b311b0a4b8734281e6e80c5",
               "fullname":"Root Root",
               "type":null,
               "user":"root",
               "timestamp":1529944842645
            },
            "fullName":"Team Room",
            "unreadTotal":2,
            "isActive":"true",
            "type":"t",
            "user":"team-be95776cd7c3950f190f0e21ea1e4848fec3874f",
            "room":"be95776cd7c3950f190f0e21ea1e4848fec3874f",
            "admins":[
               "root"
            ],
            "status":"team",
            "timestamp":1529944842647,
            "isFavorite":true
         },
         {
           "lastMessage":{
              "msg":"",
              "isSystem":true,
              "options":{
                 "fullname":"Root Root",
                 "type":"type-add-team-user"
              },
              "msgId":"5b311b0a4b8734281e6e90f5",
              "fullname":"Root Root",
              "type":null,
              "user":"root",
              "timestamp":1529944842505
           },
           "fullName":"qdqsdsqdqsdqsdsqdsqd",
           "unreadTotal":0,
           "isActive":"true",
           "type":"s",
           "user":"space-be95776cd7c3950f190f0e21ea1e4848fed8995",
           "room":"be95776cd7c3950f190f0e21ea1e4848fed8995",
           "admins":[
              "root"
           ],
           "status":"team",
           "timestamp":1529944842515,
           "isFavorite":true
        }
      ]
    },
    mocks: {
      $t: () => {},
      $constants : chatConstants,
      mq: 'desktop'
    }
  });

  it('4 displayed contacts', () => {
    expect(cmp.findAll(ChatContact)).toHaveLength(4);
  });

  it('Selected contact to be "Test User "', () => {
    expect(cmp.findAll('.contact-list-room-item.selected')).toHaveLength(1);
    expect(cmp.find('.contact-list-room-item.selected').find(ChatContact).props().userName).toBe('testuser');
  });

  it('Room "Team Room" has unread total', () => {
    expect(cmp.findAll('#chat-users .contact-list-room-item .unreadMessages')).toHaveLength(1);
    expect(cmp.find('#chat-users .contact-list-room-item .unreadMessages').text()).toBe('2');
  });

});
