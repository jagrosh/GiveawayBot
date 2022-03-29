const cors_url = 'https://api.allorigins.win/get?url='

// Query string parsing
function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&#]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function avatarFormat(user) {
    return 'https://cdn.discordapp.com/avatars/' + user.id + '/' + user.avatar + '.png';
}

function infoFormat(title, value) {
    return '<p>' + title + '<br><code>' + value + '</code><br></p>'
}

function userFormat(user) {
    return '<img src="' + avatarFormat(user) + '" alt="avatar"> '+'<b>' + user.username + '</b>#' + user.discrim + ' (' + user.id + ')<br>';
}

// Document Parsing
function parseAndShowDocument(data, url) {
    // filter html and parse json
    var obj = JSON.parse(data.contents.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;').split('\n').join('<br>'));
    var ended = new Date(obj.giveaway.end * 1000);
    var text = "<h2>Giveaway Summary</h2>"
             + infoFormat("Prize", obj.giveaway.prize)
             + infoFormat("Winners", obj.giveaway.num_winners)
             + infoFormat("Ended", ended.toLocaleString())
             + infoFormat("Giveaway ID", obj.giveaway.id);
    text += "<br><h2>Winners</h2>";
    obj.winners.forEach(user => text += userFormat(user));
    text += "<br><h2>Entrants</h2>"
    obj.entries.forEach(user => text += userFormat(user));
    
    // update
    $('#output').html(text + '<br><br><a class="button" href="'+url+'">Download Giveaway Summary JSON</a>');
}

// Loading doc and parsing
$(document).ready(function() {
    var loc = getParameterByName('giveaway')
    var url = "https://cdn.discordapp.com/attachments/"+loc+"/giveaway_summary.json";
    if(loc) {
        $.ajax({
            url: cors_url + url ,
            headers: {'x-requested-with': 'GiveawayBot Giveaway Summary'},
            method: 'GET',
            success: function(data) { parseAndShowDocument(data, url) },
            error: function( jqXHR, textStatus, errorThrown) {
                $('#output').html('Failed to load <b>' + url + '</b> : ' + errorThrown);
            }
        });
    } else {
        $('#output').html('No giveaway summary provided.');
    }
});