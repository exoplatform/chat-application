export default function(msg, highlight, emojis) {
  if (!msg || !msg.trim().length) {
    return msg;
  }
  let message = '';
  // '&lt;' represents the '<'
  // '&gt;' represents the '>'
  const lines = msg.split(/<br\s*\/?>|&lt;br\s*\/?&gt;/);

  lines.forEach( (line, index) => {
    line = $('<div />').html(line).text();
    const words = line.split(/\s|&nbsp;/g);
    words.forEach(w => {
      // check link
      if (w.indexOf('http:') === 0 || w.indexOf('https:') === 0 || w.indexOf('ftp:') === 0) {
      // retrieve the URL of the message
        try {
          const urlMessage = (new URL(w));
          if (urlMessage) {
          // check image link
            if (w.endsWith('.jpg') || w.endsWith('.png') || w.endsWith('.gif') || w.endsWith('.JPG') || w.endsWith('.PNG') || w.endsWith('.GIF')) {
              // external image link
              if (!(urlMessage.host === window.location.host)) {
                w = `<a href="${w}" target='_blank'><img src="${w}" alt="${w}"/></a>`;
              }
              else {
                w = `<a href="${w}"><img src="${w}" alt="${w}"/></a>`;
              }
              // check  external links
            } else if (!(urlMessage.host === window.location.host)) {
              w = `<a href="${w}" target='_blank'>${w}</a>`;
            }
            else {
              w = `<a href="${w}">${w}</a>`;
            }
          }
        }
        catch (urlError) {
          console.error(urlError);
        }
      } else if (highlight !== '' && w.indexOf(highlight) >= 0) {
        w = w.replace(highlight, `<span class="search-highlight">${highlight}</span>`);
      } else {
        // check emoticons
        const wordCaseSensitive = w.replace(/\.|\?|!/, '');
        const wordCaseInsensitive = wordCaseSensitive.toLowerCase();
        const emoticon = emojis.find(emoticon => emoticon.keys.indexOf(wordCaseInsensitive) >= 0);
        if (emoticon) {
          w = w.replace(wordCaseSensitive, `<span class="chat-emoticon ${emoticon.class}"><span>${wordCaseSensitive}</span></span>`);
        }
      }
      message += `${w} `;
    });

    if (index !== lines.length - 1) { // avoid adding break line at last line
      message += '<br>';
    }
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
 
  quote = `<div><blockquote><span class="quote-user-name">${quoteUser}:</span>${quoteContent}</blockquote></div>`;

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
