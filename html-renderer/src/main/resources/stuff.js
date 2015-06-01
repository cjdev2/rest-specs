$(function(){
$(".path").click(function(){
   var id = $(this).attr("num");
   console.log(id);
   $("#" + id).toggle();
});
console.log("Ready");

});

