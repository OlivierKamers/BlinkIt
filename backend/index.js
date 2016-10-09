var express = require("express");
var mysql = require('mysql');
var twilio = require('twilio')(process.env.TWILIO_SID, process.env.TWILIO_API_KEY);
var tw = require('twilio');

var connection = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});
var app = express();

connection.connect(function (err) {
  if (!err) {
    console.log("Database is connected ... \n\n");
  } else {
    console.log("Error connecting database ... \n\n");
  }
});

app.get("/score/:score/:phone/:name", function (req, res) {
  console.log(new Date(), req.params);
  connection.query('INSERT INTO score SET ?, date_created = NOW();', req.params, function (err, rows) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong inserting the new score.');
    } else {
      console.log(new Date(), rows);
      beatHighScore(res, rows.insertId, req.params.score, req.params.phone)
    }
  });
});

/**
 * Send given :message to phone logged in score with id :id
 */
app.get("/message/:id_from/:id_to/:message", function (req, res) {
  console.log(new Date(), req.params);
  var id_from = req.params.id_from;
  var id_to = req.params.id_to;
  var message = req.params.message;
  connection.query('SELECT * FROM score WHERE id = ?;', id_to, function (err, rows) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong getting the highscore.');
    } else {
      console.log(new Date(), rows);
      if (rows.length == 0) {
        res.status(500).send('Something went wrong getting the highscore.');
      }
      var previousHighScore = rows[0].score;
      message = "Hello. Your score of " + previousHighScore + " has been beaten. This is what the new record holder has to say. " + message;
      createMessage(res, message, id_from, id_to);
    }
  });
});

app.post("/message_data/:id", function (req, res) {
  console.log(new Date(), req.params);
  var id = req.params.id;
  connection.query('SELECT * FROM message WHERE id = ?;', id, function (err, rows) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong getting the message.');
    } else {
      console.log(new Date(), rows);
      if (rows.length == 0) {
        res.status(500).send('Something went wrong getting the message.');
        return;
      }
      var twiml = new tw.TwimlResponse();
      console.log(new Date(), 'Calling with message ' + rows[0].message);
      twiml.say(rows[0].message, {voice: 'alice'});
      res.type('text/xml');
      res.send(twiml.toString());
    }
  });
});

function beatHighScore(res, id, score, phone) {
  connection.query('SELECT * FROM score WHERE id != ? ORDER BY score DESC, date_created ASC LIMIT 1;', id, function (err, rows, fields) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong getting the highscore.');
    } else {
      console.log(new Date(), rows);
      if (rows.length === 0) {
        res.json({
          id: id,
          beatHighScore: false
        });
        return;
      }
      var previousId = rows[0].id;
      var previousHighScore = rows[0].score;
      var previousPhone = rows[0].phone;
      console.log(new Date(), "previousHighScore: " + previousHighScore);
      res.json({
        id: id,
        beatHighScore: (score > previousHighScore) && (phone != previousPhone),
        previousId: previousId
      });
    }
  });
}

function createMessage(res, message, from, to) {
  connection.query('INSERT INTO message SET ?, date_created = NOW();', {
    from: from,
    to: to,
    message: message
  }, function (err, rows) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong inserting the new message.');
    } else {
      console.log(new Date(), rows);
      sendMessage(res, rows.insertId, to);
    }
  });
}

function sendMessage(res, message_id, to) {
  connection.query('SELECT * FROM score WHERE id = ?;', to, function (err, rows) {
    if (err) {
      console.log(new Date(), err);
      res.status(500).send('Something went wrong getting the message.');
    } else {
      console.log(new Date(), rows);
      if (rows.length == 0) {
        res.status(500).send('Something went wrong getting the message.');
      }
      var promise = twilio.makeCall({
        to: rows[0].phone, // a number to call
        from: '+34518880612', // a Twilio number you own
        url: 'https://26bb8100.ngrok.io/message_data/' + message_id // A URL containing TwiML instructions for the call
      });

      promise.then(function (call) {
        console.log(new Date(), 'Call success! Call SID: ' + call.sid);
        res.status(200).send();
      }, function (error) {
        console.error(new Date(), 'Call failed!  Reason: ' + error.message);
        res.status(500).send('Something went wrong sending the message.');
      })
    }
  });

}

app.listen(3000);