
var BASE_PORT;
var HOST_NAME;
var POLL_FREQ;
var IS_POLLING;
var POLLING_LIST = [];
var VM_NUM = 5;
var counter = 0;

$(document).ready(function() {
  BASE_PORT = parseInt($('#port_num').val());
  HOST_NAME = $('#host_name').val();
  POLL_FREQ = parseInt($('#poll_freq').val());
  IS_POLLING = false;
  for (var i = 1; i <= VM_NUM; i++) {
    POLLING_LIST[i] = false;
  }
  $('button').click(buttonClick);
});

function buttonClick(event) {
  var elem = $(event.target);
  var num = elem.data("num");
  if (elem.hasClass("start")) {
    startVM(num);
  } else {
    stopVM(num);
  }
}

function startVM(num) {
  //console.log(url);
  //$('#frosty-' + num + '-btn')
  //  .removeClass('start')
  //  .addClass('stop')
  //  .html('STOP');
  //startPolling(num);
  $.getJSON(buildURL(num, "start"), function(data) {
    $('#frosty-' + num + '-btn')
      .removeClass('start')
      .addClass('stop')
      .html('STOP');
    startPolling(num);
  }).fail(function() {
    alert("Could not launch VM for frosty-" + num);
  });
}

function stopVM(num) {
  //console.log(url);
  //$('#frosty-' + num + '-btn')
  //  .removeClass('stop')
  //  .addClass('start')
  //  .html('START');
  //stopPolling(num);
  $.getJSON(buildURL(num, "stop"), function(data) {
    $('#frosty-' + num + '-btn')
      .removeClass('stop')
      .addClass('start')
      .html('START');
    stopPolling(num);
  }).fail(function() {
    alert("Could not stop VM for frosty-" + num);
  });
}

function startPolling(num) {
  POLLING_LIST[num] = true;
  $('td.frosty-' + num)
    .addClass('in-progress');
  $($('td.frosty-' + num + '.in-progress')
    .get(0)).addClass('animate');
  if (!IS_POLLING) {
    IS_POLLING = true;
    $('input').prop('disabled', true);
    poll();
  }
}

function stopPolling(num) {
  POLLING_LIST[num] = false;
  var flag = false;
  $('.frosty-' + num + '.animate').removeClass('animate');
  $('.frosty-' + num + '-vmlog').html("");
  $('.frosty-' + num + '-hlog').html("");
  $('.frosty-' + num + '-vmip').html("");
  for (var i = 0; i < VM_NUM; i++) {
    if (POLLING_LIST[i]) {
      flag = true;
      break;
    }
  }
  if (!flag) {
    IS_POLLING = false;
    $('input').prop('disabled', false);
  }
}

function poll() {
  for (var num = 1; num <= VM_NUM; num++) {
    if (!POLLING_LIST[num]) { continue; }
    pollCall(num);
  }
  setTimeout(poll, POLL_FREQ);
}

function pollCall(num) {
  $.getJSON(buildURL(num, "status"), function(data) {
    updateStatus(num, data);
  });
}

function updateStatus(num, data) {
  var vm = data.virtual_machine;
  var g = data.gossip;
  var ig = data.in_group;
  var me = data.master_elected;
  var h = data.hadoop_started;
  if (vm != null) { updateCell(num, 'start-vm', vm); }
  if (g != null) { updateCell(num, 'start-gossip', g); }
  if (ig != null) { updateCell(num, 'join-group', ig); }
  if (me != null) { updateCell(num, 'elect-master', me); }
  if (h != null) { updateCell(num, 'launch-hadoop', h); }
  $('td.frosty-' + num).removeClass('animate');
  $($('td.frosty-' + num + '.in-progress')
    .get(0)).addClass('animate');
}

function updateCell(num, state, value) {
  $('.' + state + ' td.frosty-' + num)
    .removeClass('in-progress')
    .addClass('done')
    .html(value);
  if (state === "start-vm" && $('.frosty-' + num + '-vmlog').html() === "") {
    $.getJSON(buildURL(num, "vmaddr"), function(data) {
      var ip = data.addr;
      var url = "logger.html?vmaddr=" + ip;
      var vmlink = '<a href="' + url + '" target="_blank">VM</a>';
      var hlink = '<a href="http://' + ip + ':50070" target="_blank">Hadoop</a>';
      $('.frosty-' + num + '-vmlog').html(vmlink);
      $('.frosty-' + num + '-hlog').html(hlink);
      $('.frosty-' + num + '-vmip').html(ip);
    });
  }
}

function buildURL(num, call) {
  var port = BASE_PORT + num;
  var uri = "/" + call + "?callback=?";
  var url = "http://" + HOST_NAME + ":" + port + uri;
  console.log(url);
  return url;
}
