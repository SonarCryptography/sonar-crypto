using System.Security.Cryptography;
using System.Text;

namespace CryptoTestProject;

public static class Encryption
{
    private const string SecretKey = "D}MT*}ajeTjr<]%j(q424,3uNfD@X#";

    /// <summary>
    /// Uses ECB mode with hard-coded key - VULNERABILITY
    /// </summary>
    public static string EncryptWithEcb(string plainText)
    {
        var aes = Aes.Create();
        
        // Hard-coded key
        aes.Key = Encoding.UTF8.GetBytes(SecretKey).ToArray();
        
        // Insecure mode
        aes.Mode = CipherMode.ECB;

        var encryptor = aes.CreateEncryptor();
        var plainBytes = Encoding.UTF8.GetBytes(plainText);
        var encryptedBytes = encryptor.TransformFinalBlock(plainBytes, 0, plainBytes.Length);
        
        return Encoding.UTF8.GetString(encryptedBytes);
    }
}