using NUnit.Framework;

namespace vstest;

public class Tests
{
    [Test]
    public void TestValid()
    {
        Assert.Pass("Test is valid.");
    }

    [Test]
    public void Test1()
    {
        Assert.That(2, Is.EqualTo(1));
    }

    [Test]
    public void Test2()
    {
        throw new System.InvalidOperationException("Test-exception");
    }

    [Test]
    public void Test3()
    {
        System.Console.WriteLine("Console-output message");
        Assert.Fail();
    }

    [TestCase(1)]
    [TestCase(2)]
    public void SupClassTest(int testData)
    {
        SupClass.Method(testData);
    }

    private class SupClass
    {
        public static void Method(int testData)
        {
            Other(testData);
        }

        private static void Other(int testData)
        {
            Assert.That(testData, Is.EqualTo(1));
        }
    }
}