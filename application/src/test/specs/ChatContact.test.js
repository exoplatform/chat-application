import { shallow } from 'vue-test-utils';
import ChatContact from '../../main/webapp/vue-app/components/ChatContact';

describe('ChatContact.test.js', () => {
  let cmp;

  beforeEach(() => {
    cmp = shallow(ChatContact, {
      propsData: {
        name : 'Thomas Delhoménie',
        userName: 'thomas',
        status: 'away',
        list: false,
        type: 'u',
        isCurrentUser: true
      }
    });
  });

  it('equals username to thomas', () => {
    expect(cmp.findAll('.contactLabel')).toHaveLength(1);
    expect(cmp.find('.contactLabel').text()).toBe('Thomas Delhoménie');
  });

  it('emits exo-chat-status-changed event when calling setStatus method', () => {
    // Given
    const stub = jest.fn();
    cmp.vm.$on('exo-chat-status-changed', stub);

    // When
    cmp.vm.setStatus('away');

    // Then
    expect(stub).toBeCalled();
  });
});
