public class Codec {
 
   public static final void main( String... args)
   {
       for ( String arg : args )
       {
           System.out.println( bitString( arg ) );
       }
   }
 
   public static String bitString( String source )
   {
      StringBuffer s = new StringBuffer();
      s.append( '\'' );
      for ( byte b : source.getBytes() )
      {
          s.append( Integer.toBinaryString( b ) );
      }
      s.append( '\'' );
      s.append( 'B' );
      return s.toString();
   }
  
}