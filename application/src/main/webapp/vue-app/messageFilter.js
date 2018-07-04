export default function(msg, highlight, emojis) {
  if(!msg || !msg.trim().length) {
    return msg;
  }
  let message = '';
  const words = msg.split(' ');
  
  words.forEach(w => {
    // check link
    if (w.indexOf('http:') === 0 || w.indexOf('https:') === 0 || w.indexOf('ftp:') === 0) {
      // check image link
      if (w.endsWith('.jpg') || w.endsWith('.png') || w.endsWith('.gif') || w.endsWith('.JPG') || w.endsWith('.PNG') || w.endsWith('.GIF')) {
        w = `<a href="${w}" target='_blank'><img src="${w}" alt="${w}"/></a>`;
      } else {
        w = `<a href="${w}" target='_blank'>${w}</a>`;
      }
    } else if (highlight !== '' && w.indexOf(highlight) >= 0) {
      w = w.replace(highlight, `<span class="search-highlight">${highlight}</span>`);
    } else {
      // check emoticons
      const wordCaseSensitive = w.replace(/\.|\?|!/, '');
      const wordCaseInsensitive = wordCaseSensitive.toLowerCase();
      const emoticon = emojis.find(emoticon => emoticon.keys.indexOf(wordCaseInsensitive) >= 0);
      if(emoticon) {
        w = w.replace(wordCaseSensitive, `<span class="chat-emoticon ${emoticon.class}"><span>${wordCaseSensitive}</span></span>`);
      }
    }
    message += `${w} `;
  });

  //check quote
  if (message.indexOf('[quote=') > -1) {
    message = checkQuotes(message);
  }

  return message;
}

const QUOTE_START = '[quote=';
const QUOTE_END = '[/quote]';


function transformQuote(quote) {
  const quoteUser = quote.slice(QUOTE_START.length, quote.indexOf(']'));
  const quoteContent = quote.slice(quote.indexOf(']') + 1, quote.indexOf(QUOTE_END)).trim();
 
  quote = `<blockquote><span class="quote-user-name">${quoteUser}:</span>${quoteContent}</blockquote>`;

  return quote;
}

function checkQuotes(message) {
  let quote = message.slice(message.lastIndexOf(QUOTE_START));
  quote = quote.slice(0, quote.indexOf(QUOTE_END) + QUOTE_END.length);

  message = message.replace(quote, transformQuote(quote));

  if (message.indexOf('[quote=') > -1) {
    message = checkQuotes(message);
  }

  return message;
}