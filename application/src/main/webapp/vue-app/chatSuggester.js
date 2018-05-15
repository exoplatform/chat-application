export default function (element) {
  console.log($(element))
  console.log($('.chatApplicationContainer'))
  $('#add-room-suggestor').suggester({
    type: 'tag',
    plugins: ['remove_button'],
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
    renderMenuItem: function () {
      return '<div class="avatarMini"></div>';
    },
    sortField: [{field: 'order'}, {field: '$score'}],
    providers: {
      
    }
  });
}
