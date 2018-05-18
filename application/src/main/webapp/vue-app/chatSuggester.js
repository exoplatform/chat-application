import * as ChatServices from './chatServices';
const USER_LIMIT = 10;

export default (element, userSettings, vm) => {
  $('#add-room-suggestor').suggester({
    type: 'tag',
    create: false,
    createOnBlur: false,
    highlight: false,
    openOnFocus: false,
    sourceProviders: ['exo:chatuser'],
    valueField: 'name',
    labelField: 'fullname',
    searchField: ['fullname', 'name'],
    closeAfterSelect: true,
    dropdownParent: 'body',
    renderMenuItem (item, escape) {
      const avatar = ChatServices.getUserAvatar(item.name);
      const defaultAvatar = '/chat/img/user-default.jpg';
      return `
        <div class="avatarMini">
          <img src="${avatar}" onerror="this.src='${defaultAvatar}'">
        </div>
        <div class="user-name">${escape(item.fullname)} (${item.name})</div>
        <div class="user-status"><i class="chat-status-${item.status}"></i></div>
      `;
    },
    renderItem(item) {
      vm.newRoom.participants.push(item);
      return '';
    },
    sortField: [{field: 'order'}, {field: '$score'}],
    providers: {
      'exo:chatuser': function (query, callback) {
        if (!query.length) { 
          return callback(); 
        }
        ChatServices.getChatUsers(userSettings, query, USER_LIMIT).then(data => {
          const users = data.users.filter(user => user.name !== userSettings.username); // remove current user
          callback(users);
        });
      }
    }
  });
};
