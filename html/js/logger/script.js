
var keeprunning_members = false;
var keeprunning_events = false;
var event_counter = 0;
var polling_freq = 1000;
var polling_url = "localhost:8001";
var member_uri = "members";
var event_uri = "events";
var auto_scroll = true;

var EVENT_CALL = "event";
var MEMBER_CALL = "member";

$(document).ready(function() {
  var addr = getUrlParameter("vmaddr");
  $('#polling_url').val(addr + ":8001");
  $('#members_button').click(call_members);
  $('#events_button').click(call_events);
  $('#toggle_members_polling').click(toggle_ajax_members);
  $('#toggle_events_polling').click(toggle_ajax_events);
  $('#toggle_auto_scroll').click(toggle_auto_scroll);
  $('.toggle_events').click(toggle_events_visibility);
  $('#clear_data').click(clear_data);
});

function getUrlParameter(sParam)
{
  var sPageURL = window.location.search.substring(1);
  var sURLVariables = sPageURL.split('&');
  for (var i = 0; i < sURLVariables.length; i++)
  {
    var sParameterName = sURLVariables[i].split('=');
    if (sParameterName[0] == sParam)
    {
      return sParameterName[1];
    }
  }
}

function clear_data() {
  // Clear member data
  $('.members .member_table tbody').replaceWith($('<tbody></tbody>'));
  // Clear event data
  $('.events .event_table tbody').replaceWith($('<tbody></tbody>'));
}

function toggle_events_visibility(e) {
  var elem = $(this);
  var type = elem.attr("data-type");
  if (type === "debug") {
    $('.event_table').toggleClass("hide_debug");
    $('#toggle_debug').toggleClass("hidden");
  } else if (type === "info") {
    $('.event_table').toggleClass("hide_info");
    $('#toggle_info').toggleClass("hidden");
  } else if (type === "warning") {
    $('.event_table').toggleClass("hide_warning");
    $('#toggle_warning').toggleClass("hidden");
  } else if (type === "error") {
    $('.event_table').toggleClass("hide_error");
    $('#toggle_error').toggleClass("hidden");
  }
}

function toggle_auto_scroll() {
  if (auto_scroll) {
    $('#toggle_auto_scroll').html("Enable Auto Scroll");
    auto_scroll = false;
  } else {
    $('#toggle_auto_scroll').html("Disable Auto Scroll");
    auto_scroll = true;
  }
}

function update_polling_info() {
  var val = $('#polling_freq').val();
  if (val !== "") {
    polling_freq = parseInt(val, 10);
  }
  val = $('#polling_url').val();
  if (val !== "") {
    polling_url = val;
  }

  if (keeprunning_events || keeprunning_members) {
    $('#polling_freq, #polling_url').prop("disabled", true).addClass("disabled");
  } else {
    $('#polling_freq, #polling_url').prop("disabled", false).removeClass("disabled");
  }
}

function toggle_ajax_members() {toggle_ajax(MEMBER_CALL)}
function toggle_ajax_events() {toggle_ajax(EVENT_CALL)}

function toggle_ajax(call_type) {
  if (call_type === MEMBER_CALL) {
    if (keeprunning_members) {
      keeprunning_members = false;
      $('#toggle_members_polling').html("Start Members");
      $('#members_button').prop("disabled", false);
    } else {
      keeprunning_members = true;
      $('#toggle_members_polling').html("Stop Members");
      $('#members_button').prop("disabled", true);
      poll_members();
    }
  } else if (call_type === EVENT_CALL) {
    if (keeprunning_events) {
      keeprunning_events = false;
      $('#toggle_events_polling').html("Start Events");
      $('#events_button').prop("disabled", false);
    } else {
      keeprunning_events = true;
      $('#toggle_events_polling').html("Stop Events");
      $('#events_button').prop("disabled", true);
      poll_events();
    }
  }
  update_polling_info();
}

function poll_members() { ajax_poll(MEMBER_CALL); }
function poll_events() { ajax_poll(EVENT_CALL); }

function ajax_poll(call_type) {
  update_polling_info();
  var uri;
  if (call_type === MEMBER_CALL && keeprunning_members) {
    uri = member_uri;
  } else if (call_type === EVENT_CALL && keeprunning_events) {
    uri = event_uri;
  } else {
    return;
  }

  make_call(call_type, uri);

  if (call_type === MEMBER_CALL) {
    setTimeout(poll_members, polling_freq);
  } else if (call_type === EVENT_CALL) {
    setTimeout(poll_events, polling_freq);
  }

}

function call_members() { make_call(MEMBER_CALL, member_uri); }
function call_events() { make_call(EVENT_CALL, event_uri); }

function make_call(call_type, uri) {
  console.log("Making call - uri: " + uri + " type: " + call_type);
  $.getJSON("http://" + polling_url + "/" + uri + "?callback=?", function(data) {
    refresh_data(call_type, data);
  })
    .fail(function() {
      console.log("Request fail: " + call_type);
    });
}

function refresh_data(call_type, data) {
  if (call_type === MEMBER_CALL) {
    refresh_data_member(data);
  } else if (call_type === EVENT_CALL) {
    refresh_data_event(data);
  }
}

function refresh_data_member(data) {
  var tbody = $('<tbody></tbody>');
  var row = $('<tr></tr>');
  var col = $('<td></td>');
  var title = $('<th></th>');

  // Add members
  var mem_list = data.member_list;
  for (var i = 0; i < mem_list.length; i++) {
    var addr = col.clone().html(mem_list[i].address);
    var beat = col.clone().html(mem_list[i].heartbeat);
    var time = col.clone().html(mem_list[i].timestamp);
    var curr_row = row.clone().append(addr, beat, time);
    if (mem_list[i].is_me) {
      curr_row.addClass("me");
    }
    if (mem_list[i].is_master) {
      curr_row.addClass("master");
    }
    tbody.append(curr_row);
  }

  // Add time stamp
  var label = col.clone().html("Last updated at:");
  var d = new Date();
  var t = d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();
  var time = col.clone().html(t);
  tbody.append(row.clone().append(label, time, col.clone()));

  // Replace existing table
  $('.members .member_table tbody').replaceWith(tbody);
}

function refresh_data_event(data) {
  var tbody = $('.events .event_table tbody');
  var row = $('<tr></tr>');
  var col = $('<td></td>');

  var events = data.event_list;
  for (var i = 0; i < events.length; i++) {
    var counter = col.clone().html(event_counter);
    var event_type = events[i].log_type;
    var type = col.clone().html(event_type);
    var event = col.clone().html(events[i].message);
    var row_curr = row.clone().append(counter, type, event);
    row_curr.attr("data-event-type", event_type);
    tbody.prepend(row_curr);
    event_counter++;
  }

  if (auto_scroll) {
    var container = $('.events .event_table tbody');
//    container.animate({ scrollTop: container[0].scrollHeight }, "slow");
    container.animate({ scrollTop: 0 }, "slow");
  }
}