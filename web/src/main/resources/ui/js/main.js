$(
    function () {
        $("#menu").menu();
        $(".draggable").draggable();
        $(".resizable").resizable();
        $("#editor textarea").on("keyup", function (q) {
            console.log(q);

            var value = $(this).val()
            console.log(value);
            var value1 = value.trim().split(/\s+/).join("/")

            console.log(value1);

            if (value.endsWith(" ")) {
                $.getJSON("/phrase/" + value1 + "?data={}", "", function (data) {
                    console.log(data);

                    {
                        var items = [];
                        $.each($(data.continue).slice(0, 100), function (key, val) {
                            console.log(key);
                            console.log(val);
                            console.log(val.word);
                            items.push("<span>" + val.word + "</span>");
                        });

                        $("#prompt").html(
                            $("<span/>", {
                                "class": "my-new-list",
                                html: items.join("<span> </span>")
                            })
                        )

                    }

                    {
                        var items = [];

                        $.each($(data.theSame).slice(0, 10), function (key, val) {
                            items.push("<tr><td>" + val.word + "</td><td>" + val.weight + "</td></tr>");
                        });

                        $("#promptTheSame .data").html(
                            $("<table/>", {
                                "class": "",
                                html: items.join("")
                            })
                        )
                    }
                })
            } else if (value.endsWith(".")) {
                $("<p/>", {
                    text: value
                }).appendTo("#text")

                $(this).val("")

                $.getJSON("/phrase/" + value1 + "?data={}", "", function (data) {
                    console.log(data); {
                        var items = [];

                        $.each($(data.next).slice(0, 4), function (key, val) {
                            items.push("<tr><td>" + val.word + "</td><td>" + val.weight + "</td></tr>");
                        });

                        $("#promptNext .data").append(
                            $("<table/>", {
                                "class": "",
                                html: items.join("")
                            })
                        )
                    }

                })

            }

        });
    }
);
