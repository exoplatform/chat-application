export default function(msg) {
  if(!msg || !msg.trim().length) {
    return msg;
  }
  let currentIndex = 0;
  let message = '';

  const words = msg.split(/\s|\.|\?|!|$/);
  words.forEach(w => {
    // check link
    if(w) {
      currentIndex += w.length;
      if (w.indexOf('http:') ===0 || w.indexOf('https:') === 0 || w.indexOf('ftp:') === 0) {
        w = `<a href="${w}" target='_blank'>${w}</a>`;
        message += w;
      } else {
        // check emoticons
        const wordLowercase = w.toLowerCase();
        const emoticon = EMOTICONS.find(emoticon => emoticon.keys.indexOf(wordLowercase) >= 0);
        if(emoticon) {
          message += `<span class="chat-emoticon ${emoticon.class}"></span>`;
        } else {
          message += w;
        }
      }
    } else {
      message += msg.charAt(currentIndex);
      currentIndex++;
    }
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
