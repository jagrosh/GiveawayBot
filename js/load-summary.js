const cors_url = 'https://api.allorigins.win/get?url='
const valid_ids = ['415263528090337295', '415263552505643018', '415263570528567307', '415263592984870922', '415263623099973655', '415263645212213269', '415263667114737675', '958156545814822932']

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
    return '<p>' + title + '<br><code>' + value + '</code><br></p>';
}

function userFormat(user) {
    return '<img src="' + avatarFormat(user) + '" alt=""> '+'<b>' + user.username + '</b>#' + user.discrim + ' (' + user.id + ')';
}

// Document Parsing
function parseAndShowDocument(data, url) {
    // filter html and parse json
    var obj = JSON.parse(data.contents.split('&').join('&amp;').split('<').join('&lt;').split('>').join('&gt;').split('\n').join('<br>'));
    var ended = new Date(obj.giveaway.end * 1000);
    var text = "<h2>Giveaway Summary</h2>"
             + obj.giveaway.desc == null ? "" : infoFormat("Description", obj.giveaway.desc)
             + infoFormat("Prize", obj.giveaway.prize)
             + infoFormat("Host", userFormat(obj.giveaway.host))
             + infoFormat("Winners", obj.giveaway.num_winners)
             + infoFormat("Ended", ended.toLocaleString())
             + infoFormat("Giveaway ID", obj.giveaway.id);
    text += "<br><h2>Winners</h2>";
    if(obj.winners)
        obj.winners.forEach(user => text += userFormat(user) + "<br>");
    text += "<br><h2>Entrants</h2>"
    if(obj.entries)
        obj.entries.forEach(user => text += userFormat(user) + "<br>");
    
    // update
    $('#output').html(text + '<br><br><br><a class="button" href="'+url+'">Download Giveaway Summary JSON</a>');
}

// Loading doc and parsing
$(document).ready(function() {
    var loc = getParameterByName('giveaway')
    if(!loc)
    {
        $('#output').html('No giveaway summary provided.');
        return;
    }
    var base_id = loc.split('/')[0]
    if(valid_ids.indexOf(base_id) == -1)
    {
        $('#output').html('Invalid giveaway summary provided.');
        return;
    }
    //var url = "https://cdn.discordapp.com/attachments/"+loc+"/giveaway_summary.json";
    var url = "https://summary-api.giveawaybot.party/?giveaway=" + loc;
    if(loc) {
        $.ajax({
            url: cors_url + encodeURIComponent(url) ,
            headers: {'x-requested-with': 'GiveawayBot Giveaway Summary'},
            method: 'GET',
            success: function(data) { parseAndShowDocument(data, url) },
            error: function( jqXHR, textStatus, errorThrown) {
                $('#output').html('Failed to load <b>' + url + '</b> : ' + errorThrown + '<br><h3><a href="' + url + '">Click here to download Giveaway Summary JSON</a></h3>');
            }
        });
    } else {
        $('#output').html('No giveaway summary provided.');
    }
});