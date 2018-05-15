<template>
  <div class="contactListContainer">
    <div class="contactFilter">
      <i class="uiIconSearchLight"></i>
      <input type="text" placeholder="Filter people, spaces...">
    </div>
    <div class="listHeader">
      <dropdown-select>
        <span slot="toggle">{{ sortFilter }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <li v-for="filter in sortByDate" slot="menu" :key="filter" @click="selectSortFilter(filter)"><a href="#">{{ filter }}</a></li>
      </dropdown-select>
      <dropdown-select>
        <span slot="toggle">{{ typeFilter }}</span>
        <i slot="toggle" class="uiIconArrowDownMini"></i>
        <li v-for="filter in filterByType" slot="menu" :key="filter" @click="selectTypeFilter(filter)"><a href="#">{{ filter }}</a></li>
      </dropdown-select>
    </div>
    <div class="contactList">
      <div v-for="contact in contacts" :key="contact.user" :class="{selected: selected.user == contact.user}" class="contact-list-item isList" @click="selectContact(contact)">
        <chat-contact :list="true" :type="contact.type" :user-name="contact.user" :name="contact.fullName" :status="contact.status"></chat-contact>
        <div v-show="contact.unreadTotal > 0" class="unreadMessages">{{ contact.unreadTotal }}</div>
        <div :class="{'is-fav': contact.isFavorite}" class="uiIcon favorite" @click.stop="toggleFavorite(contact)"></div>
      </div>
    </div>
  </div>
</template>

<script>
import ChatContact from './ChatContact.vue';
import DropdownSelect from './DropdownSelect.vue';
export default {
  components: {ChatContact, DropdownSelect},
  props: {
    contacts: {
      type: Array,
      default: function() { return [];}
    },
    selected: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data : function() {
    return {
      sortByDate: ['Recent','Unread'], // TODO add to locale
      sortFilter: 'Recent',
      filterByType: ['All','People','Rooms','Spaces','Favorites'],
      typeFilter: 'All'
    };
  },
  computed: {
    statusStyle: function() {
      return this.contactStatus === 'inline' ? 'user-available' : 'user-invisible';
    }
  },
  methods: {
    selectContact(contact) {
      this.$emit('exo-chat-contact-selected', contact);
      //let room = this.contacts.findIndex(c => c.user == contact.user);
      //this.contacts[room].unreadTotal = 0;
      contact.unreadTotal = 0;
    },
    toggleFavorite(contact) {
      contact.isFavorite = !contact.isFavorite;
    },
    selectSortFilter(filter) {
      this.sortFilter = filter;
    },
    selectTypeFilter(filter) {
      this.typeFilter = filter;
    }
  }
};
</script>