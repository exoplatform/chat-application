package org.benjp.chat.stress 
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class PublicChatSimulation extends Simulation {

	val httpConf = httpConfig
		.baseURL("http://www.acme.com")
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
		.connection("keep-alive")
		.userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:19.0) Gecko/20100101 Firefox/19.0")

	val scnInit = scenario("Scenario : Init")
		.exec(http("server init")
			.get("/chatServer/")
		)
		.exec(http("drop db")
			.get("/chatServer/dropDB")
			.queryParam("db", "gatling")
			.queryParam("passphrase", "chat")
		)
		.exec(http("init db")
			.get("/chatServer/initDB")
			.queryParam("db", "gatling")
			.queryParam("passphrase", "chat")
		)
		.repeat(300, "userId") {
			exec(http("create user")
				.get("/chatServer/createDemoUser")
				.queryParam("username", "user${userId}")
				.queryParam("passphrase", "chat")
			)			
		}
		.exec(http("get token")
			.get("/chatServer/getToken")
			.queryParam("username", "user1")
			.queryParam("passphrase", "chat")
			.check(regex("""token": "(.+)",""").saveAs("token"))
		)
		.exec(http("get room")
			.get("/chatServer/getRoom")
			.queryParam("user", "user1")
			.queryParam("token", "${token}")
			.queryParam("targetUser", "user2")
			.queryParam("isAdmin", "false")
			.check(regex("""(.+)""").saveAs("room"))
		)			
		exec(http("send message")
			.get("/chatServer/send")
			.queryParam("user", "user1")
			.queryParam("targetUser", "user2")
			.queryParam("token", "${token}")
			.queryParam("room", "${room}")
			.queryParam("message", "This is gatlin message")
		)			
		.exec(http("init db")
			.get("/chatServer/initDB")
			.queryParam("db", "gatling")
			.queryParam("passphrase", "chat")
		)

	val iUser = new java.util.concurrent.atomic.AtomicInteger(0)

	val scnSendMessages = scenario("Scenario : Messages")
		.exec((session:Session) => session.setAttribute("userId", iUser.getAndIncrement))
		.exec(http("get token")
			.get("/chatServer/getToken")
			.queryParam("username", "user${userId}")
			.queryParam("passphrase", "chat")
						//  "token": "45b2b",
			.check(regex("""token": "(.+)",""").saveAs("token"))
		)
		.exec(http("get room")
			.get("/chatServer/getRoom")
			.queryParam("user", "user${userId}")
			.queryParam("token", "${token}")
			.queryParam("targetUser", "user499")
			.queryParam("isAdmin", "false")
			.check(regex("""(.+)""").saveAs("room"))
		)			
		.repeat(500, "cpt") {
			exec(http("send message")
				.get("/chatServer/send")
				.queryParam("user", "user${userId}")
				.queryParam("targetUser", "user499")
				.queryParam("token", "${token}")
				.queryParam("room", "${room}")
				.queryParam("message", "This is gatlin message : ${cpt}")
			)			
			.pause(0 milliseconds, 10 milliseconds)
		}

	val iUserRead = new java.util.concurrent.atomic.AtomicInteger(0)

	val scnReadMessages = scenario("Scenario : Read")
		.exec((session:Session) => session.setAttribute("userId", iUserRead.getAndIncrement))
		.exec(http("get token")
			.get("/chatServer/getToken")
			.queryParam("username", "user${userId}")
			.queryParam("passphrase", "chat")
						//  "token": "45b2b",
			.check(regex("""token": "(.+)",""").saveAs("token"))
		)
		.exec(http("get room")
			.get("/chatServer/getRoom")
			.queryParam("user", "user${userId}")
			.queryParam("token", "${token}")
			.queryParam("targetUser", "user499")
			.queryParam("isAdmin", "false")
			.check(regex("""(.+)""").saveAs("room"))
		)			
		.repeat(500, "cpt") {
			exec(http("read messages")
				.get("/chatServer/send")
				.queryParam("user", "user${userId}")
				.queryParam("targetUser", "user499")
				.queryParam("token", "${token}")
				.queryParam("room", "${room}")
			)			
			.pause(0 milliseconds, 10 milliseconds)
		}


	setUp(
		scnInit.users(1).protocolConfig(httpConf),
		scnSendMessages.users(200).ramp(20).delay(5).protocolConfig(httpConf),
		scnReadMessages.users(200).ramp(20).delay(160).protocolConfig(httpConf)
		)
}