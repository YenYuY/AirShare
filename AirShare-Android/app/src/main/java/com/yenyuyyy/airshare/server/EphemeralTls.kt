package com.yenyuyyy.airshare.server

import java.math.BigInteger
import java.net.InetAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

internal data class EphemeralTlsMaterial(
    val context: SSLContext,
    val certificate: X509Certificate,
    val certifiedIpAddresses: Set<String>,
)

internal object EphemeralTlsFactory {
    fun create(advertisedAddress: InetAddress?): EphemeralTlsMaterial {
        val random = SecureRandom()
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"), random)
        }.generateKeyPair()
        val subject = X500Name("CN=AirShare Local Transfer")
        val now = System.currentTimeMillis()
        val certifiedAddresses = certifiedAddresses(advertisedAddress)
        val certificateBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger(159, random).setBit(158),
            Date(now - FIVE_MINUTES_MILLIS),
            Date(now + ONE_DAY_MILLIS),
            subject,
            keyPair.public,
        )
            .addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            .addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
            .addExtension(
                Extension.extendedKeyUsage,
                false,
                ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth),
            )
            .addExtension(
                Extension.subjectAlternativeName,
                false,
                GeneralNames(subjectAlternativeNames(certifiedAddresses.values)),
            )
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(certificateBuilder.build(signer)).apply {
            checkValidity()
            verify(keyPair.public)
        }

        val password = randomPassword(random)
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null)
            setKeyEntry(
                "airshare",
                keyPair.private,
                password,
                arrayOf<java.security.cert.Certificate>(certificate),
            )
        }
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, password)
        }.keyManagers
        val context = SSLContext.getInstance("TLS").apply {
            init(keyManagers, null, random)
        }
        return EphemeralTlsMaterial(context, certificate, certifiedAddresses.keys.toSet())
    }

    private fun certifiedAddresses(advertisedAddress: InetAddress?): LinkedHashMap<String, String> {
        val addresses = linkedMapOf<String, String>()
        fun add(address: InetAddress) {
            val text = requireNotNull(address.hostAddress).substringBefore('%')
            addresses[text] = text
        }
        add(InetAddress.getByName("127.0.0.1"))
        add(InetAddress.getByName("::1"))
        advertisedAddress
            ?.takeUnless { it.isAnyLocalAddress }
            ?.let(::add)
        return addresses
    }

    private fun subjectAlternativeNames(ipAddresses: Collection<String>): Array<GeneralName> {
        return buildList {
            add(GeneralName(GeneralName.dNSName, "localhost"))
            ipAddresses.forEach { add(GeneralName(GeneralName.iPAddress, it)) }
        }.toTypedArray()
    }

    private fun randomPassword(random: SecureRandom): CharArray {
        val bytes = ByteArray(24).also(random::nextBytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }.toCharArray()
    }

    private const val FIVE_MINUTES_MILLIS = 5L * 60L * 1_000L
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1_000L
}
