public class Digits implements Comparable {

	public int position;
	public int digit;
	
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return position - ((Digits)arg0).position;
	}

}
