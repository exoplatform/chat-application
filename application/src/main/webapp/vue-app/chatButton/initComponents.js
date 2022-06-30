import ChatButton from './components/ChatButton.vue';

const components = {
  'chat-button': ChatButton,
};

for (const key in components) {
  Vue.component(key, components[key]);
}