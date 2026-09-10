import os
import pandas as pd
from sqlalchemy import create_engine
from dotenv import load_dotenv

load_dotenv()

db_user = os.getenv('DB_USER')
db_password = os.getenv('DB_PASSWORD')
db_host = os.getenv('DB_HOST')
db_port = os.getenv('DB_PORT')
db_name = os.getenv('DB_NAME')

engine = create_engine(f'postgresql+psycopg2://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}')
total_money = pd.read_sql("SELECT SUM(total_price) AS money_moved FROM ORDERS WHERE status = 'FILLED'", engine)
print(f"${total_money['money_moved'].values[0]:,.2f}")