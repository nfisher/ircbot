(ns ircbot.core)

(use 'lamina.core 'aleph.tcp 'gloss.core)

(def chan "#bot-testing")
(def nick (str "clj-bot-" (rand-int 1000)))

(defn irc-cmd [cmd args] (str cmd " " args "\r\n"))
(defn msg-cmd [txt] (irc-cmd "PRIVMSG" (str chan " :" txt)))
(defn user-cmd [] (irc-cmd "USER" (str nick " 0 * :tutorial bot")))
(defn join-cmd [] (irc-cmd "JOIN" chan))
(defn nick-cmd [] (irc-cmd "NICK" nick))
(defn pong-cmd [token] (irc-cmd "PONG" token))

(defn handshake [] [(nick-cmd) (user-cmd) (join-cmd)])

(defn parse-ping [s] (let [[_ b] (re-matches #"PING :(.+)" s)] b))

(defn -main
  "I don't do a whole lot."
  [& args]
  ) 
 
(defn irc-connect []
  (let [ch @(tcp-client {:host "irc.freenode.net"
                         :port 6667
                         :frame (string :utf-8 :delimiters ["\r\n"] :as-str true)})
        setup-irc (fn []
                    (siphon (->> ch fork (map* parse-ping)
                                 (filter* (complement nil?)) 
                                 (map* pong-cmd)) ch)
                    (apply enqueue ch (handshake)))]
    (receive-all (fork ch) println)
   (setup-irc)
    ch))

(defonce irc-channel (irc-connect))

(defn interact [cmd args]
    (enqueue irc-channel (irc-cmd cmd args)))

(defn send-msg [txt]
  (enqueue irc-channel (msg-cmd txt)))

(defn reload []
  (use 'ircbot.core :reload))
