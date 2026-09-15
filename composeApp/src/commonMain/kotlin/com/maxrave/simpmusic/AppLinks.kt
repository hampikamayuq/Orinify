package com.maxrave.simpmusic

/**
 * The links this app answers to.
 *
 * One definition, read by everything that writes a link and everything that reads one: the
 * manifest's intent filter, the playback notification, the home-screen widgets, the pasted-link
 * translation and the Desktop registrars. These were eight separate string literals, and a deep
 * link only works when both sides agree — a mismatch raises no error anywhere, it just produces a
 * link that quietly does nothing.
 */
object AppLinks {
    /**
     * The fork's own scheme, deliberately not `simpmusic`. Both apps can be installed at once, and
     * two apps claiming one scheme turn every link into a "which app?" chooser — or hand it to the
     * wrong one.
     */
    const val SCHEME = "orinify"

    /**
     * Upstream's site still serves `https://simpmusic.org/app/...` links and this app still opens
     * them, so the host is kept. That one does raise a chooser while both apps are installed, which
     * is the honest outcome: the link really can be opened by either.
     */
    const val WEB_HOST = "simpmusic.org"

    fun uri(path: String) = "$SCHEME://$path"
}
