import * as ChatServices from './chatServices';
const USER_LIMIT = 10;

export default (element, userSettings, vm) => {
  element = $(element);
  
  //init suggester
  element.suggester({
    type: 'tag',
    create: false,
    preload: true,
    createOnBlur: false,
    highlight: false,
    openOnFocus: false,
    sourceProviders: ['exo:chatuser'],
    valueField: 'name',
    labelField: 'fullname',
    searchField: ['fullname', 'name'],
    closeAfterSelect: true,
    dropdownParent: 'body',
    hideSelected: true,
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
      if(!vm.newRoom.participants.find(participant => participant.name === item.name)) {
        vm.newRoom.participants.push(item);
      }
      return false;
    },
    sortField: [{field: 'order'}, {field: '$score'}],
    providers: {
      'exo:chatuser': function (query, callback) {
        if (!query.length) {
          return callback(); 
        }
        ChatServices.getChatUsers(userSettings, query, USER_LIMIT).then(data => {
          let users = data.users.filter(user => user.name !== userSettings.username); // remove current user
          users = data.users.filter(user => !vm.newRoom.participants.find(participant => participant.name === user.name)); // avoid  
          callback(users);
        });
      }
    }
  });

  //clear suggester
  element.suggester('setValue', '');
  element[0].selectize.renderCache['item'] = {};

  if(vm.selected.participants) {
    vm.selected.participants.forEach(participant => {
      element[0].selectize.addOption(participant);
      element[0].selectize.addItem(participant.name);
      element[0].selectize.removeOption(participant.name);
    });
  }
};
