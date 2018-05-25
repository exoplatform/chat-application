export default function(msg) {
  const words = msg.split(' ');
  let message = '';

  words.forEach(w => {
    // check link
    if (w.indexOf('http:') ===0 || w.indexOf('https:') === 0 || w.indexOf('ftp:') === 0) {
      w = `<a href="${w}" target='_blank'>${w}</a>`;
      message += w;
      return;
    }
    // check emoticons
    EMOTICONS.forEach(elm => {
      if(elm.keys.indexOf(w) > -1) {
        w = `<span class="chat-emoticon ${elm.class}"></span>`;
        message += w;
        return;
      }
    });
  });
  return message;
}


const EMOTICONS = [
  { 
    keys: [':-)', ':)'],
    class: 'emoticon-smile'
  },
  { 
    keys: [':-(', ':('],
    class: 'emoticon-sad'
  },
  { 
    keys: [';-)', ';)'],
    class: 'emoticon-wink'
  },
  { 
    keys: [':-|', ':|'],
    class: 'emoticon-speechless'
  },
  { 
    keys: [':-o', ':o'],
    class: 'emoticon-surprise'
  },
  { 
    keys: [':-p', ':p', ':-P', ':P'],
    class: 'emoticon-smile-tong'
  },
  { 
    keys: [':-d', ':d', ':-D', ':D'],
    class: 'emoticon-flaugh'
  },
  { 
    keys: ['(y)', '(yes)'],
    class: 'emoticon-speechless'
  },
  { 
    keys: ['(n)', '(no)'],
    class: 'emoticon-speechless'
  }
];
