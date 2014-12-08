// Script.js
var PORT_NUM;
var POLL_FREQ;
var KEEP_POLLING;
var GOSSIP_RUNNING = false;

$(document).ready(function() {
  $('#start_btn').click(start);
  $('#stop_btn').click(stop);
  $('.logger_frame').load(function () {
    $(this).height($(this).contents().height());
//    $(this).width($(this).contents().width());
  });
});

function start() {
  PORT_NUM = $('#port_num').val();
  POLL_FREQ = $('#poll_freq').val();
  disable("#port_num", true);
  disable("#poll_freq", true);
  var url = "http://localhost:" + PORT_NUM + "/start?callback=?";
  $.getJSON(url, function(data) {
    KEEP_POLLING = true;
    disable("#stop_btn", false);
    disable("#start_btn", true);
    $('.status_container').addClass("animate");
    poll_status();
  }).fail(function() {
    disable("#port_num", false);
    disable("#poll_freq", false);
    alert("Failed to start");
  });
}

function stop() {
  var url = "http://localhost:" + PORT_NUM + "/stop?callback=?";
  $.getJSON(url, function(data) {
  }).fail(function() {
    alert("Failed to stop");
  });

  KEEP_POLLING = false;
  disable("#stop_btn", true);
  disable("#start_btn", false);
  $('.status_container').removeClass("animate");
  $('.status_container .state').removeClass("done").addClass("in_progress");
  $('#port_num').prop("disabled", false);
  disable("#port_num", false);
  disable("#poll_freq", false);
  poll();
}

function poll_status() {
  if (KEEP_POLLING) {
    poll();
    setTimeout(poll_status, POLL_FREQ);
  } else {

  }
}

function poll() {
  var url = "http://localhost:" + PORT_NUM + "/status?callback=?";
  $.getJSON(url, function(data) {
    update_state(data);
  }).fail(function() {
    console.log("Failed to get status");
  });
}

function disable(elem, enable) {
  $(elem).prop("disabled", enable);
}

function update_state(data) {
  set_done($('.state.vm'), data.virtual_machine);
  set_done($('.state.g'), data.gossip);
  set_done($('.state.ig'), data.in_group);
  set_done($('.state.ms'), data.master_elected);
  set_done($('.state.h'), data.hadoop_started);
  var in_progress = $('.status_container.animate .in_progress');
  in_progress.removeClass('animate');
  $(in_progress.get(0)).addClass('animate');
  if (!GOSSIP_RUNNING && data.gossip) {
    launchIFrame();

  }
}

function launchIFrame() {
  var url = "http://localhost:" + PORT_NUM + "/vmaddr?callback=?";
  $.getJSON(url, function(data) {
    GOSSIP_RUNNING = true;
    addr = data.addr;
//    var win = window.open(url);
//    if(win) {
//      //Browser has allowed it to be opened
//      win.focus();
//    } else {
//      //Broswer has blocked it
//      alert('Please allow popups for this site');
//    }
    var url = "logger.html?vmaddr=" + addr;
    var link = '<a class="logger_link" href="' + url  + '" target="_blank">Go to VM Logger</a>';
    $('.container').append(link);
    url = "http://" + addr + ":50070";
    link = '<a class="hadoop_link" href="' + url  + '" target="_blank">Go to Hadoop Logger</a>';
    $('.container').append(link);
  }).fail(function() {
    console.log("Failed to get vm address");
  });
}

function set_done(elem, is_done) {
  if (is_done && elem.hasClass("in_progress")) {
    elem.addClass("done").removeClass("in_progress");
  } else if (!is_done && elem.hasClass("done")) {
    elem.removeClass("done").addClass("in_progress");
  }
}

